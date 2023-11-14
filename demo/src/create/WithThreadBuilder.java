package create;

public class WithThreadBuilder {
    public static void main(String[] args) throws InterruptedException {
        createTwo();
    }

    public static void createOne() throws InterruptedException {
        System.out.println("This is main Thread: " + Thread.currentThread().getName());
        Thread.Builder builder = Thread.ofVirtual().name("My thirst virtual thread");
        Runnable task = ()-> {
            System.out.println("this is task thread");
        };
        Thread thread =  builder.start(task);
        System.out.println("thread name is: " + thread.getName());
        thread.join();
    }

    public static void createTwo() throws InterruptedException {
        Thread.Builder builder = Thread.ofVirtual().name("virtual-thread-", 0);
        Runnable task = ()->{
          System.out.println("this is task thread: " + Thread.currentThread().getName() + " running...");
        };
        // name: virtual-thread-0
        Thread thread1 = builder.start(task);
        thread1.join();
        System.out.println(thread1.getName() + " terminated");
        // name: virtual-thread-1
        Thread thread2 = builder.start(task);
        thread2.join();
        System.out.println(thread2.getName() + " terminated");
    }
}
