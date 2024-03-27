# 虚拟线程出现之前存在的问题

服务器开发基于thread-per-request方式，由于Java平台的线程实现方式，JVM中的一个线程对应OS的一个线程。OS的线程数是有限的，而且线程的创建和销毁开销比较大，即使使用线程池也没有很好得解决高并发的需求。

# 虚拟线程的实现方式

Virtual Threads的实现依赖JVM的协作调度器和Fork/Join框架，通过将多个virtual threads映射到OS上少量的线程实现高并发，类似于OS的虚拟内存与物理内存的原理。

# 虚拟线程的优缺点
适用IO密集型操作，不适用CPU密集型操作。
## 优点：

1. 更高的性能：虚拟线程的创建和销毁开销小，突破了OS层面的数量限制
2. 更好的伸缩性：可以创建百万级甚至亿级的虚拟线程，能满足高并发场景
3. 更低的资源消耗：轻量级，相比线程的实现方式，占用更少的系统资源

## 缺点：

1. 学习成本：需要了解Java的并发模型以及新的API使用

2. 潜在的问题：新特性，可能会有不稳定的问题

# 虚拟线程分类
## 调度线程（scheduled virtual thread）
平台线程是由操作系统调度，而虚拟线程由Java Runtime 调度。当Java Runtime调度一个虚拟线程时，他会将虚拟线程挂载到一个平台线程，再由操作系统调度这个平台线程，此时这个平台线程称为携带者（carrier）。当发生IO阻塞操作时，虚拟线程会从携带者上卸载下来，Java Runtime可以再将别的虚拟线程挂载到携带者上继续上述操作。
## 固定线程（pinned virtual thread）
在发生IO阻塞操作时不能从携带者上卸载下来的虚拟线程，称为固定线程。
在以下两种情况中，线程会固定在携带者上：
	+ 虚拟线程运行在synchronized关键字修饰的代码块或者方法中
	+ 虚拟线程运行在native方法中或foreign function中
所以，为了扩展性，尽量使用ReentrantLock代替synchronized

# 虚拟线程使用指南

## 在thread-per-request风格中，写简单的同步的阻塞IO代码
以下代码不会因为使用虚拟线程而获得收益
```
CompletableFuture.supplyAsync(info::getUrl, pool)
   .thenCompose(url -> getBodyAsync(url, HttpResponse.BodyHandlers.ofString()))
   .thenApply(info::findImage)
   .thenCompose(url -> getBodyAsync(url, HttpResponse.BodyHandlers.ofByteArray()))
   .thenApply(info::setImageData)
   .thenAccept(this::process)
   .exceptionally(t -> { t.printStackTrace(); return null; });
```
使用同步代码，简单的阻塞性IO代码，能获得更好的收益
```
try {
   String page = getBody(info.getUrl(), HttpResponse.BodyHandlers.ofString());
   String imageUrl = info.findImage(page);
   byte[] data = getBody(imageUrl, HttpResponse.BodyHandlers.ofByteArray());   
   info.setImageData(data);
   process(info);
} catch (Exception ex) {
   t.printStackTrace();
}
```
## 一个虚拟线程对应一个并发任务，不要试图池化虚拟线程
不要使用共享线程池
```
Future<ResultA> f1 = sharedThreadPoolExecutor.submit(task1);
Future<ResultB> f2 = sharedThreadPoolExecutor.submit(task2);
// ... use futures
```
建议一个虚拟线程对应一个任务
```
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
   Future<ResultA> f1 = executor.submit(task1);
   Future<ResultB> f2 = executor.submit(task2);
   // ... use futures
}
```
## 使用信号量（Semaphore）限制并发
平台线程的限制方式
```
ExecutorService es = Executors.newFixedThreadPool(10);
...
Result foo() {
    try {
        var fut = es.submit(() -> callLimitedService());
        return f.get();
    } catch (...) { ... }
}
```
虚拟线程的限制方式
```
Semaphore sem = new Semaphore(10);
...
Result foo() {
    sem.acquire();
    try {
        return callLimitedService();
    } finally {
        sem.release();
    }
}
```

## 不要在Thread-Local变量中缓存昂贵的可重用的对象
1. 由于一个虚拟线程对应一个任务，所以ThreadLocal中存的对象是有隔离性的，无法像线程池中的平台线程那样重用对象
2. 由于虚拟线程数是百万级别甚至更多，所以ThreadLocal中存太多数据会导致大量内存的占用

## 避免长时间或者频繁的固定在平台线程上
由于固定（pinning）发生时会阻塞OS的线程，所以代码中需要尽量减少此操作。
如果是由于经常调用的地方使用synchronized关键字导致的固定（pinning），请使用ReentrantLock代替Synchronized。如果你确定synchronized关键字导致的固定时间非常小或者使用不频繁，可以不用替换成ReentrantLock。

不建议的方式：
```
synchronized(lockObj) {
    frequentIO();
}
```

推荐的方式
```
lock.lock();
try {
    frequentIO();
} finally {
    lock.unlock();
}
```