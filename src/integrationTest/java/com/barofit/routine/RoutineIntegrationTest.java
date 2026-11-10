package com.barofit.routine;

import com.barofit.RoutineApplication;
import com.barofit.testsupport.BarofitJwtKeyTestSupport;
import com.barofit.testsupport.BarofitPostgresTestSupport;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** DB 통합 테스트. 이 레포 자신의 Spring 컨텍스트와 공유 PostgreSQL 컨테이너를 쓴다 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = RoutineApplication.class)
@ActiveProfiles("test")
@Import({BarofitPostgresTestSupport.class, BarofitJwtKeyTestSupport.class})
public @interface RoutineIntegrationTest {}
