package com.barofit.routine.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WeeklyGoalProgressServiceTest {

  // 포트 1번은 어떤 로컬 서버도 열 수 없어 연결이 즉시 거부된다.
  private static final String UNREACHABLE = "http://127.0.0.1:1";
  private static final LocalDate MONDAY = LocalDate.of(2026, 3, 9);

  private HttpServer identityServer;
  private HttpServer workoutServer;

  @AfterEach
  void tearDown() {
    if (identityServer != null) identityServer.stop(0);
    if (workoutServer != null) workoutServer.stop(0);
  }

  /** JDK 내장 HttpServer로 identity/workout을 대신한다 (WireMock 등 새 의존성 추가 없이). */
  private HttpServer fakeServer(String path, String responseBody) {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          path,
          exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var out = exchange.getResponseBody()) {
              out.write(body);
            }
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOExceptionForTest(e);
    }
  }

  private static String baseUrl(HttpServer server) {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private static class UncheckedIOExceptionForTest extends RuntimeException {
    UncheckedIOExceptionForTest(Throwable cause) {
      super(cause);
    }
  }

  @Test
  void get_rejectsNonMondayStart() {
    var service = new WeeklyGoalProgressService(UNREACHABLE, UNREACHABLE);

    assertThatThrownBy(() -> service.get(UUID.randomUUID(), LocalDate.of(2026, 3, 10), "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.INVALID_PLAN.code());
  }

  @Test
  void get_throwsProfileUnavailable_whenIdentityUnreachable() {
    var service = new WeeklyGoalProgressService(UNREACHABLE, UNREACHABLE);

    assertThatThrownBy(() -> service.get(UUID.randomUUID(), MONDAY, "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.PROFILE_UNAVAILABLE.code());
  }

  @Test
  void get_throwsProfileUnavailable_whenResponseIsForDifferentUser() {
    UUID userId = UUID.randomUUID();
    identityServer =
        fakeServer(
            "/api/v1/users/me",
            "{\"id\":\""
                + UUID.randomUUID()
                + "\",\"isOnboarded\":true,"
                + "\"profile\":{\"weeklyWorkoutDays\":3}}");
    var service = new WeeklyGoalProgressService(baseUrl(identityServer), UNREACHABLE);

    assertThatThrownBy(() -> service.get(userId, MONDAY, "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.PROFILE_UNAVAILABLE.code());
  }

  @Test
  void get_throwsOnboardingRequired_whenNotOnboarded() {
    UUID userId = UUID.randomUUID();
    identityServer =
        fakeServer(
            "/api/v1/users/me",
            "{\"id\":\"" + userId + "\",\"isOnboarded\":false," + "\"profile\":{}}");
    var service = new WeeklyGoalProgressService(baseUrl(identityServer), UNREACHABLE);

    assertThatThrownBy(() -> service.get(userId, MONDAY, "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.ONBOARDING_REQUIRED.code());
  }

  @Test
  void get_throwsWorkoutUnavailable_whenWorkoutServiceUnreachable() {
    UUID userId = UUID.randomUUID();
    identityServer =
        fakeServer(
            "/api/v1/users/me",
            "{\"id\":\""
                + userId
                + "\",\"isOnboarded\":true,"
                + "\"profile\":{\"weeklyWorkoutDays\":3}}");
    var service = new WeeklyGoalProgressService(baseUrl(identityServer), UNREACHABLE);

    assertThatThrownBy(() -> service.get(userId, MONDAY, "t"))
        .isInstanceOf(RoutineException.class)
        .extracting(ex -> ((RoutineException) ex).getErrorCode().code())
        .isEqualTo(RoutineErrorCode.WORKOUT_UNAVAILABLE.code());
  }

  @Test
  void get_returnsProgressClampedToGoal_onHappyPath() {
    UUID userId = UUID.randomUUID();
    identityServer =
        fakeServer(
            "/api/v1/users/me",
            "{\"id\":\""
                + userId
                + "\",\"isOnboarded\":true,"
                + "\"profile\":{\"weeklyWorkoutDays\":2}}");
    // weeklyGoal=2이지만 완료일은 3일 -> goalProgress는 2로 clamp돼야 한다.
    workoutServer =
        fakeServer(
            "/api/v1/workout-sessions/weekly-progress",
            "{\"weekStart\":\"2026-03-09\",\"completedDates\":"
                + "[\"2026-03-09\",\"2026-03-10\",\"2026-03-11\"]}");
    var service = new WeeklyGoalProgressService(baseUrl(identityServer), baseUrl(workoutServer));

    var result = service.get(userId, MONDAY, "t");

    assertThat(result.weeklyGoal()).isEqualTo(2);
    assertThat(result.completedCount()).isEqualTo(3);
    assertThat(result.goalProgress()).isEqualTo(2);
    assertThat(result.completedDates())
        .containsExactly(
            LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 11));
  }
}
