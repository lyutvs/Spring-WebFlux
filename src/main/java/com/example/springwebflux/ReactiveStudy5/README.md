## Reactive Programming: Flux

#### 사전 지식들

``` java
@GetMapping("/event/{id}")
Mono<Event> event(@PathVariable long id) {
return Mono.just(new Event(id, "event: " + id));
}

result: {id: 123, "value": "event:123"}
```

json 형식으로 return 되는데 어떻게 된걸까?
> Spring 의 Message Converter 가 동작할 때 @RestController를 추가했으니
> Restful 방식으로 결과를 return 한다.
>
> 실제 Client 에 Return 하는 Data는 <Event;> object인데 이 Object가
> Java Bean 스타일로 돼있으면, 이 Object의 Getter로 부터 끌어올 수 있는
> Property 정보를 활용해 일정한 Rule에 따른 json 결과로 만들어준다.

### [Flux]

> Reactive Style로 Mono<> 를 사용했는데, 만약 전통적인 MVC
> 라고 생각을 하면, 이는 Event Object를 만들어서 그것을 return 하고, Event
> 가 한개가 아니라 여러개면 Collection에 넣어서 return 하면 된다.
>
> 즉 Flux는 이와 (Collection) 비슷하게 여러개 인것. 어떠한 Object를 여러개
> 보내는 것이다.

```java
@GetMapping("/events")
Flux<Event> events(){
        return Flux.just(new Event(1L,"event1")
        ,new Event(2L,"event2"));
        }
```

이와같이 Flux는 Mono와는 다르게 .just에도 여러개를 넣을 수 있다.

```json
이와같이 return 된다.
[{"id":1,"value":"event1"},{"id":2,"value":"event2"}]
```


즉, 복수개 (0 ~ 1개인 모노와 다르게 0~N개)의 데이터를 보낼 수 있다. 하나의 Collection에 담겨진걸 return 하는것과 비슷하다.

_그렇다면 Mono 안에 List를 넣는건 어떨까? Mono<List;>_

```java
@GetMapping("/events/{id}")
Mono<List<Event>>eventsMono(@PathVariable long id){
    List<Event> list = Arrays.asList(new Event(1L,"event1")
                            ,new Event(2L,"event2"));
    return Mono.just(list);
}
```
결과가 Flux와 다르지 않다. 여러개의 데이터를 List 형식으로 하나의
Collection으로 만들어, 이 하나의 List를 Mono에 담아서 사용해도 문제가 없다.


### Flux의 .fromIterable()

Flux는 여러개의 Object를 보낼 수 있는데, 그럼 Flux<Obejct> 안에 List<Object>는 ?
의미가 다르다 !

_**Flux.fromIterable()_**  을 사용한다.
```java
@GetMapping("/events/FluxIterable")
Flux<Event> eventFlux() {
    List<Event> list = Arrays.asList(new Event(1, "event1")
    , new Event(2, "event2"));

    return Flux.fromIterable(list);
}
```
.fromIterable 안에 List Collection을 넣으면, List는 Iterable Interface
의 sub interface. 거기로부터 Flux Datastream을 만든다 !

그 후 .map() (Flux.fromIterable().map()) 을 사용할 수 있다.

_**.fromIterable() 을 사용해 list를 넣는것과 List를 만들어서 Flux.just(list)와 차이는 무엇일까?**_
> [1] .just(list)를 사용하면, list가 하나의 데이터로 하여, mono에 mapping을 하던, list 안에 있는
> 하나 하나의 entity 레벨에서 작업을 수행하는데는, Reactor Library에 있는 Operator 들을 사용할 수 없다.
> 물론 Collection (List)를 통째로 action을 줄 수는 있다.
>
> [2] HTTP Stream을 지원하려면, 즉, 원래는 요청에 따른 결과를 하나로 보내는게 아닌
> chunk 단위로 나누어서 보내는 방식을 사용할 때는, Flux를 이용하는게 편하다.

그렇다면 일반 HTTP Request에 대한 하나의 Response가 아닌, Flux 내부 Object의
하나 하나의 요소들을 Stream 단위로 쪼개어 보내주고 싶다면 어떻게 할까

```java
@GetMapping(value = "/events/FluxIterable", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<Event> eventFluxIterable() {
    List<Event> list = Arrays.asList(new Event(1, "event1")
            , new Event(2, "event2"));

    return Flux.fromIterable(list);
}
```
MediaType을 TEXT_EVENT_STREAM_VALUE로 설정해 주어야한다.

여기서 produces의 목적 두 가지는

[1] Client가 요청을 보낸 것 에서, accept Header의 내용을 보고 Mapping 하기 위해서.

[2] Return을 하는 것이 어떤 MediaType으로 return 하냐? 를 지정할 수 있다.

이렇게 Return을 해 준다면?

```json
data:{"id":1,"value":"event1"}

data:{"id":2,"value":"event2"}
```
이와같이 Return 된다.

```json
Flux의 List로 Return 된 것, 전체가 json의 array에 묶여서 온다
[{"id":1,"value":"event1"},{"id":3,"value":"event12"}]

Stream으로 온 것
data:{"id":1,"value":"event1"}
data:{"id":2,"value":"event2"}
```

#### 그렇다면 이번에는 Flux를 이용해서, data를 stream 단위로 쪼개서 보내는데, 일정한 시간차를 두고 전송을 해 보자.

> Data를 Generate 하는 두 가지 방법
>
> [1] Flux 쪽에서 Data를 Generate
>
> [2] Data를 Generate하는 Java쪽의 Stream을 만들어 놓고, Flux에 집어넣기

1. Stream을 이용해 Data 만들기: Stream.generate();
> Stream.generate()는 Supplier를 주면, 그 Supplier를 계속 호출해
> 그 안에서 Data를 끌어온다. Supplier는 Parameter가 없고, return 값만 있다.
```java
Stream<Event> s = Stream.generate(() -> new Event(System.currentTimeMillis(), "value"));

return Flux.fromStream(s);
```
이렇게 하면 Flux.fromStream으로 그 값을 가져올 수 있다.
하지만 현재 이 코드는 **무한 스트림**이 된다.

Stream에 .limit() 을 걸거나, Flux에서 take(n) 를 해서 n개만 가져올 수 있다.
```java
1. Stream.limit()
Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value")).limit(10));

2. Flux.take()
Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value"))).take(10);
```
.take(n)가 하는 일은, n개의 request를 보내고, 그걸 Counting 하다가, .cancel()을 보내는 방식

```json
data:{"id":1625576613632,"value":"value"}
data:{"id":1625576613660,"value":"value"}
data:{"id":1625576613662,"value":"value"}
data:{"id":1625576613663,"value":"value"}
data:{"id":1625576613665,"value":"value"}
data:{"id":1625576613667,"value":"value"}
data:{"id":1625576613670,"value":"value"}
data:{"id":1625576613672,"value":"value"}
data:{"id":1625576613673,"value":"value"}
data:{"id":1625576613674,"value":"value"}
```
10개의 Data가 Stream에서 생성되어 Flux Stream에서 하나의 요소씩 json으로 return 된것을 확인할 수 있다.

너무 빠르니 .delay()를 사용해보자.
```java
return Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value")))
                   .delayElements(Duration.ofSeconds(3))
                   .take(10);
delay시간을 주었다.
```
**Delay는 Background Thread를 만들어서 별도의 Thread가 delay시간만큼 물고있다. Blocking()이 된다. 물론 Method
가 아닌, Background thread가**

하지만 이 방법은, Java에서 generate 하는 것과, Flux가 섞여있어 보기 좋지않다.

이번에는, Stream.generate()로 Java가 제공하는 기능이 아닌, Flux에서 제공하는 기능으로 Stream을 만들어보자

### Flux.generate() & sink
> Flux 자체적으로도 generate를 제공한다.
>
> [1] .generate(Consumer<>)
>
> [2] .generate(Callable<> stateSupplier)
>
> [3] .generate (Consumer<> stateConsumer)
>
> 이 중 [1], Flux 자체에 있는 Consumer<>는, 기존 Stream.generate()에 Supplier를 lambda
> 식으로 사용했다면, sink를 던지면 그 sink를 이용해 .next()를 사용한다.

> sink라는 것은, data를 계속 흘려보내는 역할이라 할 수 있다.
>
> 즉 sink를 가지고 sink.next()로 다음 data를 계속 보내주는 방식이라 할 수 있다.

```java
return Flux.<Event>generate(sink -> sink.next(new Event(System.currentTimeMillis(), "value")))
                   .delayElements(Duration.ofSeconds(1))
                   .take(10);
```
generate는 return하는 type 정보가 없기에 앞에 type hint로 <Event>를 붙힌다.

그렇다면 만약 숫자가 계속 변하는, 미리 정해지 않은걸 만들고 싶다면?

[2] .generate(Callable<> stateSupplier)를 사용한다.

> stateSupplier는 어떠한 상태 (generation 이라는 작업에서 계속 변화는 값)
> 가 있는데, 그 상태값을 갖고 generate 하고, 그 상태값을 바꿔서 return 하는 그런 역할을 한다.
>
> .generate()에 초기 상태를 생성하는 Callable 함수 한개 (stateSupplier),
> 그 초기 상태를 받아서 첫 번째 generation 작업을 돌릴 때 현재 상태와, sink해서 데이터를 보내는 generator() 를 넘긴다.

> .generate(첫 번째 생성값, (초기값, sink) -> {})

```java
return Flux.<Event, Long>generate(() -> 1L, (id, sink) -> {
                        sink.next(new Event(id, "value: " + id));
                        return id + 1;
                    })
                   .delayElements(Duration.ofSeconds(1))
                   .take(10);
```

즉 1L 은 초기의 상태값 (1)

(id, sink):  그 상태값 (id)과 sink

그 상태값과 sink를 이용해서 -> sink.next() 값을 하나 생성해서 보내고, return id + 1 다음 값을 또 들어가도록

마치 loop를 돌면서 id값을 증가시키도록 한다.

```json
결과)
data:{"id":1,"value":"value: 1"}
data:{"id":2,"value":"value: 2"}
data:{"id":3,"value":"value: 3"}
data:{"id":4,"value":"value: 4"}
data:{"id":5,"value":"value: 5"}
data:{"id":6,"value":"value: 6"}
data:{"id":7,"value":"value: 7"}
data:{"id":8,"value":"value: 8"}
data:{"id":9,"value":"value: 9"}
data:{"id":10,"value":"value: 10"}
```

### Flux.interval() 과 .zip

위에서 element를 생성하는데 delayElement를 이용하여 늦추는 방식이 아닌 interval을 사용할 수 있다.
> Flux.interval() 은 데이터를 생성을 하는데 일정한 주기를 가지고 0 부터 시작하는 값을 던져주는 방식.

```java
Flux<Event> f = Flux.generate(() -> 1L, (id, sink) -> {
            sink.next(new Event(id, "value: " + id));
            return id + 1;
        });

Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
```
이 두 가지 f와 interval을 결합해서 delayElement와 비슷한 효과를 낼 수 있다.

> 두 개의 Flux를 만들었는데 이 둘을 결합할 때 merge를 할 수 있다.
>
> 즉 Flux f 는 flux의 상태와 상태값을 바꾸는 코드를 이용해 데이터를 만드는 flux
>
> Flux interval 는 일정한 간격 단위로 숫자를 만드는 Flux.
>
> 이 두 가지 흐름 (Flux)를 조합할 수 있다.

Flux.zip()을 이용
> Flux.zip(Flux, Flux) 두 개의 stream을 사용할 수 있음.
```java
Flux.zip(f, interval);
```

**f가 어떤 간격으로 1, 2, 3, ... 데이터를 만들고, interval이 1초 간격으로 숫자를 만드는걸
한 쌍씩 만드는 것 !**

```java
Flux.zip(f, interval).map(tuple -> tuple.getT1());
```
그리고 우리는 interval이 만들어내는 숫자를 쓰고싶은게 아니고, interval 이 Data를 가져오는걸로 mapping을 한다.

tuple로 두개를 결합한거에서 T1이라는 첫 번째 Data <Event>를 가져오는 것

#### 그렇다면, interval을 delay 용도 뿐 아니라 실제 데이터로 활용하려면?
실제로 .zip은 위와같이 delay를 위해 interval을 묶고 interval에서 생성되는 element를 버리는게 아닌,
해당 값도 모두 사용하는데 많이 쓰인다.

위와 같이 {Long, String} : <Event> 객체를 리턴하듯 만들어보자.

```java
Flux<String> f = Flux.generate(sink -> sink.next("value"));
Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

return Flux.zip(f, interval).map(tuple -> new Event(tuple.getT2(), tuple.getT1() + ":" + tuple.getT1()));
```