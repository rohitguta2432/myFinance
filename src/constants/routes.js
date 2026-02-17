/**
 * Route path constants — single source of truth for all navigation.
 * Import these instead of hardcoding strings in components.
 */
export const ROUTES = {
    HOME: '/',
    ASSESSMENT: {
        STEP_1: '/assessment/step-1',
        STEP_2: '/assessment/step-2',
        STEP_3: '/assessment/step-3',
        STEP_4: '/assessment/step-4',
        STEP_5: '/assessment/step-5',
        STEP_6: '/assessment/step-6',
        COMPLETE: '/assessment/complete',
    },
};

/**
 * Total number of assessment steps.
 */
export const TOTAL_STEPS = 6;
