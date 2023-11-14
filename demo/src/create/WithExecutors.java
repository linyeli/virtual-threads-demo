package create;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *  create and run a virtual thread with the Executors.newVirtualThreadPerTaskExecutor() method
 */
public class WithExecutors {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        try(ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i < 10; i++) {
                Future<?> future = executorService.submit(() -> {
                    System.out.println(Thread.currentThread().threadId());
                    System.out.println("running thread");
                });
                future.get();
                System.out.println("task end");
            }
        }
    }
}
