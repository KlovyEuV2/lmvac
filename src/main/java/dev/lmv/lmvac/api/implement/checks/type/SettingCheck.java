package dev.lmv.lmvac.api.implement.checks.type;

import dev.lmv.lmvac.api.implement.checks.type.cooldown.Cooldown;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SettingCheck {
    String value();

    Cooldown cooldown() default Cooldown.COOLDOWN;

    String description() default "&8The developer left no information.";
    DescType descType() default DescType.OLD;
}