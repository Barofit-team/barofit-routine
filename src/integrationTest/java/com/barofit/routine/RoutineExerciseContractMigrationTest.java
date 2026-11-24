package com.barofit.routine;

import static org.assertj.core.api.Assertions.assertThat;

import com.barofit.testsupport.BarofitTestPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * V13(마운틴 클라이머 REPS)·V14(23종 템플릿 정리, 사용자 루틴 호환) 검증.
 *
 * <p>공유 PostgreSQL 컨테이너 안에 테스트 전용 DB를 새로 만들어 빈 DB 설치와 V12 → V14 업그레이드를 각각 실행한다. 운영 DB는 사용하지 않는다.
 */
class RoutineExerciseContractMigrationTest {

  private static final String OWNER = "11111111-1111-1111-1111-111111111111";
  private String database;

  @BeforeEach
  void createDatabase() throws SQLException {
    database = "routine_mig_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection admin = admin();
        Statement st = admin.createStatement()) {
      st.execute("CREATE DATABASE " + database);
    }
  }

  @AfterEach
  void dropDatabase() throws SQLException {
    try (Connection admin = admin();
        Statement st = admin.createStatement()) {
      st.execute("DROP DATABASE IF EXISTS " + database + " WITH (FORCE)");
    }
  }

  @Test
  void cleanInstallExposesAll23ReadyExercisesThroughVisibleTemplates() throws SQLException {
    flyway(null).migrate();
    try (Connection c = connect()) {
      assertThat(single(c, "SELECT count(*) FROM routine.exercise")).isEqualTo("47");
      assertThat(
              single(c, "SELECT count(*) FROM routine.exercise WHERE assessment_status = 'READY'"))
          .isEqualTo("23");
      assertThat(
              single(
                  c, "SELECT measurement_type FROM routine.exercise WHERE id = 'mountain-climber'"))
          .isEqualTo("REPS");
      assertThat(
              single(c, "SELECT measurement_type FROM routine.exercise WHERE id = 'glute-bridge'"))
          .isEqualTo("REPS");
      assertThat(
              list(
                  c,
                  "SELECT id FROM routine.exercise WHERE measurement_type = 'DURATION'"
                      + " AND assessment_status = 'READY' ORDER BY id"))
          .containsExactly("flutter-kick", "plank", "side-plank");
      assertThat(list(c, VISIBLE_TEMPLATES)).hasSize(21);
      assertThat(list(c, HIDDEN_TEMPLATES))
          .containsExactly(
              "00000000-0000-0000-0000-000000000106",
              "00000000-0000-0000-0000-000000000108",
              "00000000-0000-0000-0000-000000000109");
      assertThat(
              list(
                  c,
                  "SELECT e.id FROM routine.exercise e WHERE e.assessment_status = 'READY'"
                      + " AND NOT EXISTS (SELECT 1 FROM routine.routine_exercise re"
                      + " WHERE re.exercise_id = e.id AND re.routine_id IN ("
                      + VISIBLE_TEMPLATES
                      + "))"))
          .as("23종 모두 노출 템플릿 하나 이상에서 도달 가능")
          .isEmpty();
      assertIntegrity(c);
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000113"))
          .containsExactly("mountain-climber:16", "jumping-jack:10");
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000114"))
          .containsExactly("jumping-jack:10", "mountain-climber:20", "jump-squat:10");
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000115"))
          .containsExactly("burpee:10", "jump-squat:10", "mountain-climber:24");
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000104"))
          .containsExactly("incline-push-up:10", "push-up:10");
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000111"))
          .containsExactly("sit-up:10", "reverse-crunch:10", "dead-bug:10", "flutter-kick:30s");
    }
  }

  @Test
  void upgradeFromV12KeepsUserDataAndAppliesOnlySafeReplacements() throws SQLException {
    flyway("12").migrate();
    List<String> beforeIds;
    try (Connection c = connect();
        Statement st = c.createStatement()) {
      st.execute(SEED_FUNCTION);
      copy(st, "001", "a");
      copy(st, "102", "b");
      copy(st, "107", "c");
      copy(st, "114", "d");
      copy(st, "113", "e");
      copy(st, "102", "f");
      st.execute(
          "UPDATE routine.routine_set s SET target_duration_seconds = 45, rest_seconds = 75"
              + " FROM routine.routine_exercise re WHERE s.routine_exercise_id = re.id"
              + " AND re.routine_id = '"
              + user("d")
              + "' AND re.exercise_id = 'mountain-climber'");
      st.execute(
          "UPDATE routine.routine_exercise SET exercise_id = 'single-leg-squat' WHERE routine_id = '"
              + user("f")
              + "' AND exercise_id = 'reverse-lunge'");
      st.execute(
          "INSERT INTO routine.workout_plan(id, owner_user_id, plan_date, routine_id, created_at,"
              + " updated_at) VALUES (gen_random_uuid(), '"
              + OWNER
              + "', '2026-12-01', '"
              + user("b")
              + "', now(), now())");
      beforeIds = list(c, ROUTINE_EXERCISE_IDS);
    }

    flyway(null).migrate();

    try (Connection c = connect()) {
      assertThat(list(c, ROUTINE_EXERCISE_IDS))
          .as("routine_exercise ID·순서와 루틴 행은 삭제·재생성되지 않는다")
          .isEqualTo(beforeIds);
      assertThat(
              single(
                  c,
                  "SELECT count(*) FROM routine.routine_set s JOIN routine.routine_exercise re"
                      + " ON re.id = s.routine_exercise_id JOIN routine.routine r ON r.id = re.routine_id"
                      + " WHERE r.type = 'USER'"))
          .isEqualTo("51");
      // 변경 없는 루틴은 버전도 그대로
      assertThat(exercises(c, user("a"))).containsExactly("push-up:10", "squat:15", "plank:30s");
      assertThat(version(c, user("a"))).isEqualTo("0");
      // 안전한 교체만 적용, 사용자 목표·세트 보존
      assertThat(exercises(c, user("b")))
          .containsExactly(
              "lunge:10", "split-squat:10", "glute-bridge:10", "side-lying-leg-raise:10");
      assertThat(version(c, user("b"))).isEqualTo("1");
      // 대체 종목이 없는 prone-cobra는 지우지 않고 보존 (루틴은 계속 숨김)
      assertThat(exercises(c, user("c")))
          .containsExactly("superman:10", "bird-dog:10", "prone-cobra:30s");
      // 사용자가 바꾼 45초 목표는 20회(중급)로, 휴식 75초는 유지
      assertThat(exercises(c, user("d")))
          .containsExactly("jumping-jack:10", "mountain-climber:20", "jump-squat:10");
      assertThat(
              single(
                  c,
                  "SELECT string_agg(DISTINCT s.rest_seconds::text, ',') FROM routine.routine_set s"
                      + " JOIN routine.routine_exercise re ON re.id = s.routine_exercise_id"
                      + " WHERE re.routine_id = '"
                      + user("d")
                      + "' AND re.exercise_id = 'mountain-climber'"))
          .isEqualTo("75");
      // 측정 방식·충격이 다른 종목은 교체하지 않는다
      assertThat(exercises(c, user("e")))
          .containsExactly("march-in-place:30s", "step-jack:10", "jumping-jack:10");
      assertThat(version(c, user("e"))).isEqualTo("0");
      // 같은 대상 종목으로 두 종목이 매핑되면 중복을 만들지 않는다
      assertThat(exercises(c, user("f")))
          .containsExactly(
              "lunge:10", "single-leg-squat:10", "glute-bridge:10", "side-lying-leg-raise:10");
      // 템플릿 ID는 유지되고, 빠진 종목 4건(104·105·107·113) 외 템플릿 행은 남는다
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000104"))
          .containsExactly("incline-push-up:10", "push-up:10");
      assertThat(exercises(c, "00000000-0000-0000-0000-000000000113"))
          .containsExactly("mountain-climber:16", "jumping-jack:10");
      assertThat(single(c, "SELECT routine_id::text FROM routine.workout_plan"))
          .isEqualTo(user("b"));
      assertIntegrity(c);
    }
  }

  private static void assertIntegrity(Connection c) throws SQLException {
    assertThat(
            single(
                c,
                "SELECT count(*) FROM routine.routine_set s"
                    + " JOIN routine.routine_exercise re ON re.id = s.routine_exercise_id"
                    + " JOIN routine.exercise e ON e.id = re.exercise_id"
                    + " WHERE (e.measurement_type = 'REPS' AND (s.target_reps IS NULL"
                    + " OR s.target_duration_seconds IS NOT NULL))"
                    + " OR (e.measurement_type = 'DURATION' AND (s.target_duration_seconds IS NULL"
                    + " OR s.target_reps IS NOT NULL))"))
        .as("측정 방식과 세트 목표가 일치")
        .isEqualTo("0");
    assertThat(
            single(
                c,
                "SELECT count(*) FROM (SELECT routine_id FROM routine.routine_exercise"
                    + " GROUP BY routine_id HAVING min(exercise_order) <> 1"
                    + " OR max(exercise_order) <> count(*)) g"))
        .as("운동 순서가 1부터 연속")
        .isEqualTo("0");
    assertThat(
            single(
                c,
                "SELECT count(*) FROM routine.routine_exercise re WHERE NOT EXISTS"
                    + " (SELECT 1 FROM routine.routine_set s WHERE s.routine_exercise_id = re.id)"))
        .isEqualTo("0");
    assertThat(
            single(
                c,
                "SELECT count(*) FROM (SELECT 1 FROM routine.routine_exercise"
                    + " GROUP BY routine_id, exercise_id HAVING count(*) > 1) d"))
        .isEqualTo("0");
  }

  private static final String VISIBLE_TEMPLATES =
      "SELECT r.id FROM routine.routine r JOIN routine.routine_exercise re ON re.routine_id = r.id"
          + " JOIN routine.exercise e ON e.id = re.exercise_id WHERE r.type = 'TEMPLATE'"
          + " GROUP BY r.id HAVING bool_and(e.active AND e.assessment_status = 'READY')";

  private static final String HIDDEN_TEMPLATES =
      "SELECT r.id::text FROM routine.routine r JOIN routine.routine_exercise re"
          + " ON re.routine_id = r.id JOIN routine.exercise e ON e.id = re.exercise_id"
          + " WHERE r.type = 'TEMPLATE' GROUP BY r.id"
          + " HAVING NOT bool_and(e.active AND e.assessment_status = 'READY') ORDER BY r.id";

  // 사용자 루틴만 비교한다. 템플릿은 V14가 의도적으로 종목을 빼고 순서를 다시 매긴다(아래 템플릿 검증 참고)
  private static final String ROUTINE_EXERCISE_IDS =
      "SELECT re.id::text || ':' || re.exercise_order FROM routine.routine_exercise re"
          + " JOIN routine.routine r ON r.id = re.routine_id WHERE r.type = 'USER'"
          + " ORDER BY re.id";

  private static final String SEED_FUNCTION =
      """
      CREATE FUNCTION pg_temp.copy_template(tpl uuid, rid uuid, owner uuid) RETURNS void AS $$
      DECLARE re record; nre uuid;
      BEGIN
        INSERT INTO routine.routine(id, owner_user_id, type, name, description, difficulty,
          template_id, category)
        SELECT rid, owner, 'USER', name, description, difficulty, id, category
        FROM routine.routine WHERE id = tpl;
        FOR re IN SELECT * FROM routine.routine_exercise WHERE routine_id = tpl
          ORDER BY exercise_order LOOP
          nre := gen_random_uuid();
          INSERT INTO routine.routine_exercise(id, routine_id, exercise_id, exercise_order)
          VALUES (nre, rid, re.exercise_id, re.exercise_order);
          INSERT INTO routine.routine_set(id, routine_exercise_id, set_order, target_reps,
            target_duration_seconds, weight_kg, rest_seconds)
          SELECT gen_random_uuid(), nre, set_order, target_reps, target_duration_seconds,
            weight_kg, rest_seconds
          FROM routine.routine_set WHERE routine_exercise_id = re.id;
        END LOOP;
      END $$ LANGUAGE plpgsql
      """;

  private static void copy(Statement st, String template, String suffix) throws SQLException {
    st.execute(
        "SELECT pg_temp.copy_template('00000000-0000-0000-0000-000000000"
            + template
            + "', '"
            + user(suffix)
            + "', '"
            + OWNER
            + "')");
  }

  private static String user(String suffix) {
    return "aaaaaaaa-0000-0000-0000-00000000000" + suffix;
  }

  private static String version(Connection c, String routineId) throws SQLException {
    return single(c, "SELECT version FROM routine.routine WHERE id = '" + routineId + "'");
  }

  private static List<String> exercises(Connection c, String routineId) throws SQLException {
    return list(
        c,
        "SELECT re.exercise_id || ':' || coalesce(min(s.target_reps)::text,"
            + " min(s.target_duration_seconds) || 's') FROM routine.routine_exercise re"
            + " JOIN routine.routine_set s ON s.routine_exercise_id = re.id"
            + " WHERE re.routine_id = '"
            + routineId
            + "' GROUP BY re.id, re.exercise_id, re.exercise_order ORDER BY re.exercise_order");
  }

  private Flyway flyway(String target) {
    var config =
        Flyway.configure()
            .dataSource(url(database), container().getUsername(), container().getPassword())
            .schemas("routine")
            .createSchemas(true)
            .locations("classpath:db/migration/routine");
    if (target != null) {
      config.target(target);
    }
    return config.load();
  }

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(
        url(database), container().getUsername(), container().getPassword());
  }

  private static Connection admin() throws SQLException {
    return DriverManager.getConnection(
        container().getJdbcUrl(), container().getUsername(), container().getPassword());
  }

  private static String url(String db) {
    var c = container();
    return "jdbc:postgresql://" + c.getHost() + ":" + c.getMappedPort(5432) + "/" + db;
  }

  private static org.testcontainers.containers.PostgreSQLContainer<?> container() {
    return BarofitTestPostgres.container();
  }

  private static String single(Connection c, String sql) throws SQLException {
    List<String> rows = list(c, sql);
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  private static List<String> list(Connection c, String sql) throws SQLException {
    try (Statement st = c.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      List<String> rows = new ArrayList<>();
      while (rs.next()) {
        rows.add(rs.getString(1));
      }
      return rows;
    }
  }
}
