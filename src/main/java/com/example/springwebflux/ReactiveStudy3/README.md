## Java String WebFlux: Scheduler

```java
Publisher<Integer> pub = sub -> {
    sub.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
                sub.onNext(1);
                sub.onNext(2);
                sub.onNext(3);
                sub.onNext(4);
                sub.onNext(5);
                sub.onComplete();
        }

        @Override
        public void cancel() {

        }
    });
};
```
> Publisher [pub]를 생성.
> Subscriber sub는 onSubscriber를 호출.
> onSubscribe시에 5개 Integer를 onNext로 호출하고 onComplete한다

```java
    pub.subscribe(new Subscriber<Integer>() {
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
```
> 그 후 해당 publisher를 subscribe 했을때 동작을 정의하였다.
>
> 당연히 첫 onSubscribe때는 얼마만큼의 stream을 받을껀지에 대한 request를 넘겨야 한다.
> 여기서는 제한없는 매우 큰 수로 정의하였다.

```java
18:53:37.076 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onSubscribe
18:53:37.078 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 1
18:53:37.080 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 2
18:53:37.080 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 3
18:53:37.080 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 4
18:53:37.080 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 5
18:53:37.080 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onComplete
```
정상적으로 onSubscribe이 샐행 (request(Long.MAX_VALUE)) 후 onNext가 5번 찍히고 종료된더.

##### 여기서 잘 봐야하는건 모두 main Thread에서 찍혔다.
하나의 Thread에서 Publisher와 Subscriber, Subscription이 동작하였는데, 그렇다는건 사실은 subscribe 라는 메소드를 실행하고 onComplete
까지 모든 작업이 하나의 Thread에서 실행되었다.

------------
#### 위에 구현된 Publisher & Subscriber에 대한 main thread의 흐름을 생각해보자

1. Main method에서 한 개의 Thread가 실행이 되다가,
   pub.subscribe(new Subscriber<Integer> () ... ) 를 만들어진 Subscriber를 담아서 호출

2. Publisher<Integer> pub = sub -> { ... }
   가 호출 된다. 넘어온 Subscriber object의 onSubscribe이라는 Method를 호출하며 Subscription을 던져준다.

3. pub.subscribe 안에 정의된 onSubscribe가 호출되며 던져준 Subscription s를 받아 request(long n) 을 날린다.

4. onSubscribe 호출에 의해 넘어온 Subscription s의 request를 호출,
거기서 onNext를 실행 ~ 5회 반복 후 onComplete까지 반복. 각 각은 pub.subscribe() 안에 정의된 것들이 실행된다.

5. onComplete 이후에는 onSubscribe이 끝나는 것이기에, 그 때 위 lambda식에 있는 sub가 종료되며 Main Method가 종료된다.

> 실전에서는 Publisher & Subscriber가 하나의 Thread에서 작동하지 않는다.
>
> Event가 발생하는 경우는 Background에서 발생하는 경우가 많다.
> 예를들어, UI에서 터치 입력, Redis에서 정보 받아오기 등의 Event를 기다리고 있어야 하는데, main thread가
> blocking 돼있는 방식으로 작성하지 않는다.
>
> 즉, 실제 Program에서는 Publisher와 Subscriber를 같은 스레드 안에서 직렬적으로 실행되도록 하지 않는다.

#### Publisher와 Subscriber 사이에 일어나는 action (onNext 등) 을 별도의 Thread에서 동작을 시키고 main thread는 빠져나가도록 하고싶다.
> Scheduler 이용 !
> 1. subscribeOn()
> - Subscriber가 subscribe 하고, onSubscribe를 넘기고 request()를 넘기고, 만들어진 Data를 준비하고 전달하는 등의 작업을
> **subscribeOn()** 에 지정한 Thread 안에서 수행하도록 하는 것.
> 2. publishOn()

```java
Publisher<Integer> subOnPub = sub -> {
            ExecutorService es = Executors.newSingleThreadExecutor(); // 별도의 Thread를 생성.
            es.execute(() -> pub.subscribe(sub));
};

subOnPub.subscribe(new Subscriber<Integer>() {
    ...
}
```
newSingleThreadExecutor()로 새로운 Thread를 생성하여 main thread가 아닌, 별도의 Thread에서 같은 Flow를 작동시키도록 하였다.

subscirbeOn 같은 경우는, Publisher가 매우 느리고 (blocking I/O 연산 등) 이를 처리하는 (소비하는) Subscriber 가 매우 빠른경우 사용된다.

### 반대의 경우는 어떡할까 ?: publishOn()

> publishOn은 subscribe 하고, request 보내는 등의 상황은
> 처음 호출한 Thread (main thread)에서 진행을 하고, onNext, onComplete 등 subscriber가 Data를 받아서
> 처리하는 쪽을 별도의 Thread로 바꾼다.
>
> Data 생성은 매우 빠른데, 이를 처리하는 consumer (subscriber) 쪽의 작업이 느린경우 사용될 수 있다.

```java
Publisher<Integer> pubOnPub = sub -> {
    pub.subscribe(new Subscriber<Integer>() {
        @Override
        public void onSubscribe(Subscription s) {
            sub.onSubscribe(s);
        }

        ExecutorService es = Executors.newSingleThreadExecutor();

        /**
         * 별개의 Thread를 통해서 작동시키고싶다
         */
        @Override
        public void onNext(Integer integer) {
            es.execute(() -> sub.onNext(integer));
        }

        @Override
        public void onError(Throwable t) {
            es.execute(() -> sub.onError(t));
        }

        @Override
        public void onComplete() {
            es.execute(sub::onComplete);
        }
    });
};

pubOnPub.subscribe(new Subscriber<Integer>() {
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
```

결과:
```java
19:42:21.878 [main] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onSubscribe
19:42:21.881 [pool-1-thread-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 1
19:42:21.883 [pool-1-thread-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 2
19:42:21.883 [pool-1-thread-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 3
19:42:21.883 [pool-1-thread-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 4
19:42:21.883 [pool-1-thread-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 5
19:42:21.883 [pool-1-thread-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onComplete
```
main thread에서 빠르게 onSubscribe까지 하고, 별개의 Thread에서 생성된 Data를 처리한다.

#### newSingleThreadExecutor는 Thread pool에 오직 하나의 Thread만 존재하며, 추가적인 request는 queue에 쌓는다.
> RxJava, Reactive Streams 등 하나의 Publisher가 data를 생성해서 던져주는건, multithread 로 분리가 되어 Publisher가 onNext를 호출하지 못하도록 돼 있다. 오직
> 단일 Thread에서만 사용되도록 되어있다.

### 그렇다면 둘 다 하고싶다면 어떡할까? subscribeOn, publishOn 둘 다 별개의 Thread에서 수행되도록 하고싶다.

전체 코드
```java
Publisher<Integer> pub = sub -> {
    sub.onSubscribe(new Subscription() {
        @Override
        public void request(long n) {
            log.debug("request()");
            sub.onNext(1);
            sub.onNext(2);
            sub.onNext(3);
            sub.onNext(4);
            sub.onNext(5);
            sub.onComplete();
        }

        @Override
        public void cancel() {

        }
    });
};

Publisher<Integer> subOnPub = sub -> {
    ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory() {

        @Override
        public String getThreadNamePrefix() {
            return "subOn-";
        }
    }); // 별도의 Thread를 생성 이름 subOn.
    es.execute(() -> pub.subscribe(sub));
};

Publisher<Integer> pubOnPub = sub -> {
    subOnPub.subscribe(new Subscriber<Integer>() {
        @Override
        public void onSubscribe(Subscription s) {
            sub.onSubscribe(s);
        }

        ExecutorService es = Executors.newSingleThreadExecutor(new CustomizableThreadFactory() {

            @Override
            public String getThreadNamePrefix() {
                return "pubOn-";
            }
        });

        /**
         * 별개의 Thread를 통해서 작동시키고싶다
         */
        @Override
        public void onNext(Integer integer) {
            es.execute(() -> sub.onNext(integer));
        }

        @Override
        public void onError(Throwable t) {
            es.execute(() -> sub.onError(t));
        }

        @Override
        public void onComplete() {
            es.execute(sub::onComplete);
        }
    });
};

pubOnPub.subscribe(new Subscriber<Integer>() {
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
```
결과
```java
20:04:07.136 [subOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onSubscribe
20:04:07.140 [subOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - request()
20:04:07.140 [pubOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 1
20:04:07.143 [pubOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 2
20:04:07.143 [pubOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 3
20:04:07.143 [pubOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 4
20:04:07.143 [pubOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onNext: 5
20:04:07.143 [pubOn-1] DEBUG com.example.webflux.ReactiveStudy3.ReactiveScheduler - onComplete
```
onSubscribe & request 는 subOn에서 생성된 Thread에서 수행되고, 이후 Data를 실제로
전달하는 onNext 및 onError / onComplete은 pubOn에서 만들어진
Thread에서 수행한다.

## Flux Publisher:
```java
public static void main(String[] args) {
    Flux.range(1, 10)
        .subscribe(System.out::println);
}
```
subscribe() 안에 Object를 통째로 안만들고 하나만 넣으면
onNext()만 처리하는 lambda 식을 받는다.

Main thread에서 10개의 데이터가 나오는걸 알 수 있다.


```java
public static void main(String[] args) {
    Flux.range(1, 10)
        .publishOn(Schedulers.newSingle("pub"))
        .log()
        .subscribeOn(Schedulers.newSingle("sub"))
        .subscribe(System.out::println);
}
```
* onSubscribe()와 request()는 sub이라는 thread에서 동작 (subscribeOn), data 이동되는
onNext, onComplete 등은 pub thread에서 동작 (publishOn).

#### User Thread 와 Daemon Thread
> 자바에서 사용자가 직접 생성한 User Thread (e.s. Executors.)은 main thread
> 가 종료되더라도 같이 종료되지 않는다.
>
> Daemon Thread는 JVM이 User Thread는 하나도 남지 않고 Daemon Thread들만
> 남아있으면 강제로 종료한다. User Thread는 한개라도 남아있으면 종료되지 않는다.
> Flux.interval 같은 경우는 Daemon Thread로 만든다.

즉,
```java
public static void main(String[] args) throws InterruptedException {
    /**
     * Thread를 별도로 할당하지 않았음애도, 자동으로 별도의 Thread로 동작하는 것.
     * 1. interval: 어떠한 주기를 가지고 숫자값을 0~ 무한대로 쏴주는 것 (강제 종료 전까지)
     */

    Flux.interval(Duration.ofMillis(500))
        .subscribe(s -> log.debug("onNext:{}", s));
}
```
이런식으로 만들어 놓고 실행을 해 봤자, 동작하지 않는다 (정확히는 바로 종료된다). main thread가 종료됨에 따라 별도
스레드로 동작하던 interval은 Daemon Thread로 함께 종료된다.

```java
public static void main(String[] args) throws InterruptedException {
    /**
     * Thread를 별도로 할당하지 않았음애도, 자동으로 별도의 Thread로 동작하는 것.
     * 1. interval: 어떠한 주기를 가지고 숫자값을 0~ 무한대로 쏴주는 것 (강제 종료 전까지)
     */

    Flux.interval(Duration.ofMillis(500))
        .subscribe(s -> log.debug("onNext:{}", s));

    TimeUnit.SECONDS.sleep(5);
}
```
이런식으로 강제적으로 main thread가 5초동안 sleep 상태에 들어가도록
만들면, 5초동안 Interval에서 만들어진 데이터가 출력된다. 즉 interval thread도 5초동안 유지된다,.












