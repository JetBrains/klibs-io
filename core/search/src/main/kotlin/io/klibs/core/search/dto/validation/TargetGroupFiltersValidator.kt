package io.klibs.core.search.dto.validation

import io.klibs.core.pckg.model.TargetGroup
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

class TargetGroupFiltersValidator : ConstraintValidator<ValidTargetGroupValues, List<Map<TargetGroup, Set<String>>>> {
    override fun isValid(
        tragetGroupFilters: List<Map<TargetGroup, Set<String>>>?,
        context: ConstraintValidatorContext
    ): Boolean {
        val error = validateTargetGroupFilters(tragetGroupFilters) ?: return true
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(error).addConstraintViolation()
        return false
    }
}

@Target(AnnotationTarget.FIELD)
@Constraint(validatedBy = [TargetGroupFiltersValidator::class])
annotation class ValidTargetGroupValues(
    val message: String = "Invalid filter values",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

/**
 * Validates target-group search filters.
 * Returns null when valid, otherwise a human-readable error message.
 */
fun validateTargetGroupFilters(filters: List<Map<TargetGroup, Set<String>>>?): String? {
    filters?.forEach { group ->
        group.forEach { (targetGroup, targets) ->
            if (targetGroup == TargetGroup.Unknown) {
                return "Target group '${TargetGroup.Unknown}' is not allowed as a filter"
            }
            if (!targetGroup.targets.containsAll(targets)) {
                val invalid = targets - targetGroup.targets.toSet()
                return "Invalid targets for group '$targetGroup': $invalid"
            }
        }
    }
    return null
}