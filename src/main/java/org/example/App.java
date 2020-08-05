package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import java.sql.Time;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Hello world!
 */
public class App {

  public static void main(String[] args) throws Exception {

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new TestVerticle());

    vertx.eventBus().request("vertx://addr", "hello world", (rep) -> {

      System.out.println(Thread.currentThread().getName());
      if (!rep.failed()) {
        System.out.println(rep.result().body());
      } else {
        rep.cause().printStackTrace();
      }
    });

    //a sync send and receive over the event bus
    System.out.println(syncSend(vertx, "hello world2"));

    try {
      TimeUnit.SECONDS.sleep(5);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }

  public static String syncSend(Vertx vertx, String req) throws Exception {

    Promise<String> res = Promise.promise();
    Lock lock = new ReentrantLock();
    Condition cond = lock.newCondition();

    vertx.eventBus().request("vertx://addr", req, (rep) -> {
      lock.lock();
      System.out.println("syncSend::" + Thread.currentThread().getName());
      if (rep.failed()) {
        res.fail(rep.cause());
      } else {
        res.complete(rep.result().body().toString());
      }

      cond.signalAll();
      lock.unlock();
    });

    lock.lock();
    while (!res.future().isComplete()) {
      cond.await();
    }
    lock.unlock();
    return res.future().result();


  }

}


class TestVerticle extends AbstractVerticle {


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    System.out.println("Verticle " + getClass().getSimpleName() + " has been started");

    MessageConsumer<Object> consumer = vertx.eventBus()
        .consumer("vertx://addr").handler((msg) -> {
          System.out.println(
              "Received on eventbus - " + Thread.currentThread().getName() + "-->" + msg.body()
          );
          msg.reply("-> hello there!");
        });

    startPromise.complete();

  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    stopPromise.complete();
  }
}
