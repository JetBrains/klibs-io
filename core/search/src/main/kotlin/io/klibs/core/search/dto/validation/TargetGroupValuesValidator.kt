package io.klibs.core.search.dto.validation

import io.klibs.core.pckg.model.TargetGroup
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

class TargetGroupValuesValidator : ConstraintValidator<ValidTargetGroupValues, Map<TargetGroup, Set<String>>> {
    override fun isValid(map: Map<TargetGroup, Set<String>>?, context: ConstraintValidatorContext): Boolean {
        map?.forEach { (group, targets) ->
            if (!group.targets.containsAll(targets)) {
                return false
            }
        }
        return true
    }
}

@Target(AnnotationTarget.FIELD)
@Constraint(validatedBy = [TargetGroupValuesValidator::class])
annotation class ValidTargetGroupValues(
    val message: String = "Invalid filter values",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
