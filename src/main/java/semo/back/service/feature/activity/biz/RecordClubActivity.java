package semo.back.service.feature.activity.biz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RecordClubActivity {
    String subject();

    String successDetail() default "";

    String failureDetail() default "";
}
