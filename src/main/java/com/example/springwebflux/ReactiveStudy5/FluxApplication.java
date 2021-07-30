package com.example.springwebflux.ReactiveStudy5;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@Slf4j
@RestController
public class FluxApplication {

    @GetMapping("/event/{id}")
    Mono<Event> event(@PathVariable long id) {
        return Mono.just(new Event(id, "event: " + id));
    }

    @GetMapping("/events")
    Flux<Event> events() {
        // Flux에는 여러개를 넣을 수 있다.
        return Flux.just(new Event(1L, "event1"),
                new Event(2L, "event2"));
    }

    // 모노 안에 List를 통째로 넣는건 어떨까?
    @GetMapping("/events/{id}")
    Mono<List<Event>> eventsMono(@PathVariable long id) {
        List<Event> list = Arrays.asList(new Event(id, "event1")
                , new Event(id + 2, "event" + id + 2));
        return Mono.just(list);
    }

    // Flux는 여러개의 Object를 보낼 수 있는데, 그럼 Flux<Obejct> 안에 List<Object>는 ?
    // 의미가 다르다 !
    // Flux.fromIterable() 을 사용한다.
    @GetMapping("/events/FluxIterable")
    Flux<Event> eventFlux() {
        List<Event> list = Arrays.asList(new Event(1, "event1"),
                new Event(2, "event2"));

        // return Flux.just(list); 이런방식이 아닌,
        return Flux.fromIterable(list); // 이렇게 사용한다.
    }

    // Collection으로 한번에 return 하는게 아닌, 요소를 Stream 단위로 쪼개주고 싶다면,
    // produces = MediaType.[] , MediaType을 TEXT_EVENT_STREAM_VALUE 로 설정해 주어야한다.
    @GetMapping(value = "/events/FluxIterableStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventFluxIterable() {
        List<Event> list = Arrays.asList(new Event(1, "event1"),
                new Event(2, "event2"),
                new Event(3, "event3"));

        return Flux.fromIterable(list);
    }

    // 일정한 시간 간격을 두고 요소들을 Stream으로 전송하기
    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Event> eventFluxStream() {
        // Event Data를 10개 만들어 보자
        // [1] Stream.generate()
        /*
        Stream<Event> s = Stream.generate(() -> new Event(System.currentTimeMillis(), "value"));
        return Flux.fromStream(s);
         */

        //<------------------------------------------------------>

        /*무한 스트림 제한
        1. Stream.limit()
        Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value")).limit(10));
        2. Flux.take()
        Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value"))).take(10);
         */

        //<------------------------------------------------------>

        // delay까지 줘 보자

        /*
        return Flux.fromStream(Stream.generate(() -> new Event(System.currentTimeMillis(), "value")))
                   .delayElements(Duration.ofSeconds(3))
                   .take(10);
         */

        //<------------------------------------------------------>
        // 이번에는 Java가 제공하는 기능이 아닌, Flux에서 제공하는 기능으로 Stream을 만들어보자

        /*
        return Flux.<Event>generate(sink -> sink.next(new Event(System.currentTimeMillis(), "value")))
                   .delayElements(Duration.ofSeconds(1))
                   .take(10);
         */

        //<------------------------------------------------------>
        // 미리 생산된 데이터를 for loop 를 돌아서 가져올 수도 있는데, 그렇다면 만약 숫자가 계속 변하는, 미리 정해지 않은걸 만들고 싶다면?
        /*
        return Flux.<Event, Long>generate(() -> 1L, (id, sink) -> {
                        sink.next(new Event(id, "value: " + id));
                        return id + 1;
                    }) // Callable은 supplier와 비슷하게, return 만 존재. 첫 값은 1로 !
                   .delayElements(Duration.ofSeconds(1))
                   .take(10);
         */

        //<------------------------------------------------------>

        // 이번에는 delayElement가 아닌 다른방법을 사용해보자.
/*
        Flux<Event> f = Flux.generate(() -> 1L, (id, sink) -> {
            sink.next(new Event(id, "value: " + id));
            return id + 1;
        });
        // 값을 generate해서 받는 Flux를 만들고
        // interval을 사용하는데, interval은 데이터를 생성을 하는데 일정한 주기를 가지고 0 부터 시작하는 값을 던져주는 방식.
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
        // f와 interval을 결합을 시켜 delayElement와 비슷한 효과를 낼 수 있다.
        // Flux.zip()을 이용한다
        return Flux.zip(f, interval).map(tuple -> tuple.getT1());
 */
        // 그리고 우리는 interval이 만들어내는 숫자를 쓰고싶은게 아니고, interval 이 Data를 가져오는걸로 mapping을 한다.
        // tuple로 두개를 결합한거에서 T1이라는 첫 번째 Data <Event>를 가져오는 것

        //<------------------------------------------------------>

        Flux<String> f = Flux.generate(sink -> sink.next("value"));
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));

        return Flux.zip(f, interval).map(tuple -> new Event(tuple.getT2(), tuple.getT1() + ":" + tuple.getT1()));
    }

    public static void main(String[] args) {
        SpringApplication.run(FluxApplication.class, args);
    }

    @Data
    @AllArgsConstructor
    public static class Event {
        long id;
        String value;
    }
}
