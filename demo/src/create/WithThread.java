package create;

public class WithThread {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("This is main Thread: " + Thread.currentThread().getName());
        // call Thread.ofVirtual() method to create an instance of Thread for building virtual thread
        Thread thread = Thread.ofVirtual().start(()-> System.out.println("Hello"));
        thread.join();
    }

}
