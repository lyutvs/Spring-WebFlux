package com.example.springwebflux.ReactiveStudy3;

 import java.util.concurrent.Executors;
        import java.util.concurrent.ScheduledExecutorService;
        import java.util.concurrent.TimeUnit;

        import org.reactivestreams.Publisher;
        import org.reactivestreams.Subscriber;
        import org.reactivestreams.Subscription;

        import lombok.extern.slf4j.Slf4j;

/**
 * Flux의 Interval과 비슷한 걸 구현
 * Java에서 제공하는 newSingleThreadScheduledExecutor를 사용
 * 그리고 Flux.take()와 같은, 정해진 양의 Data를 emit하고나면 중단시키는걸 구현
 */
@Slf4j
public class IntervalEx {
    public static void main(String[] args) {
        Publisher<Integer> pub = sub -> {
            sub.onSubscribe(new Subscription() {
                int no = 0;
                volatile boolean cancelled = false;

                @Override
                public void request(long n) {
                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                    exec.scheduleAtFixedRate(() -> {
                        if (cancelled) {
                            exec.shutdown();
                        }
                        sub.onNext(no++);
                    }, 0, 300, TimeUnit.MILLISECONDS);
                }

                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        };

        /**
         * Publishing 을 control !
         * Flux.take()와 비슷하도록
         */
        Publisher<Integer> takeSub = sub -> {
            pub.subscribe(new Subscriber<Integer>() {
                int count = 0;
                Subscription subsc;

                @Override
                public void onSubscribe(Subscription s) {
                    subsc = s;
                    sub.onSubscribe(s);
                }

                @Override
                public void onNext(Integer integer) {
                    sub.onNext(integer);
                    if (count++ > 5) {
                        /**
                         * 만약 count가 10을 넘어갔다 ? cancel을 사용해보자.
                         */
                        subsc.cancel();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    sub.onError(t);
                }

                @Override
                public void onComplete() {
                    sub.onComplete();
                }
            });
        };

        takeSub.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                log.debug("onSubscribe");
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer integer) {
                log.debug("onNext: {}", integer);
            }

            @Override
            public void onError(Throwable t) {
                log.debug("onError: {}", t);
            }

            @Override
            public void onComplete() {
                log.debug("onComplete");
            }
        });
    }
}

