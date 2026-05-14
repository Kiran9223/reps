package com.reps.app.core.domain.model

enum class MealSlot(val displayName: String, val timeHint: String, val sortOrder: Int) {
    BREAKFAST("Breakfast", "7:00 – 9:00 AM", 0),
    LUNCH("Lunch", "12:00 – 1:00 PM", 1),
    DINNER("Dinner", "7:00 – 9:00 PM", 2),
    SNACKS("Snacks", "Between meals", 3)
}
