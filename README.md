## 환율 Open API 실시간 호출 프로젝트

ReentrantLock + CompletableFuture 조합 방식

### 동작 방식

모든 스레드는 메서드 진입 시 CompletableFuture 객체를 참조하고, 락 획득 시도
- 락 획득 시도 시 Leader 스레드 역할로 전환
- 락 획득 시도 실패 시 Follower 스레드 역할로 전환

리더는 캐시에 최신 환율 데이터를 업데이트하고
- completableFuture.complete() API로 대기 중인 follower 스레드를 깨움
- 컬렉션(ConcurrentHashMap)에서 completableFuture 객체 제거 (follower 스레드는 여전히 참조 가능)
- 리더가 되기 위해 획득했던 락 해제

### 이슈

[[BUG] CompletableFuture 객체 참조에 대한 타이밍 불일치로 인한 스레드 무한 대기](https://github.com/sydnyyy/fx-api-call-optimizer/issues/1)
- ReentrantLock을 제거하는 방향으로 결정 (dev 브랜치 참고)