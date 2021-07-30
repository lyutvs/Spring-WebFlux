## Java String WebFlux: Mono

> Mono에 대한 전반적인 공부
> Stream을 만들어 log를 사용하여 실제로 어떻게 동작하는지 분석한다.
> 기존의 MVC, AOP와는 다른 방식으로 동작하는 Reactive Stream을 이해한다.

### Cold Source / Hot Source
> Cold Type & Hot Type
> 어느 subscriber가 요청을 하던 동일한 결과가 가는 것 --> cold type.
> Hot Source는, 실시간으로 일어나는 user interface 등의 action 등. publisher가 기동을 시작해 100개가 날라왔으면 그 다음 101번째 부터 실시간으로 발생하는 data만 사용한다.