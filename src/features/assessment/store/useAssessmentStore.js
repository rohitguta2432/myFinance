import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAssessmentStore = create(
    persist(
        (set) => ({
            // Step 1: Personal Risk Profile
            age: 30,
            setAge: (age) => set({ age }),
            state: '', // selected Indian state
            setState: (state) => set({ state }),
            city: '', // selected city within state
            setCity: (city) => set({ city }),
            maritalStatus: '', // 'single', 'married', 'divorced', 'widowed'
            setMaritalStatus: (maritalStatus) => set({ maritalStatus }),
            dependents: 0,
            setDependents: (dependents) => set({ dependents }),
            childDependents: 0,
            setChildDependents: (childDependents) => set({ childDependents }),
            employmentType: '', // 'Salaried', 'Self-Employed', 'Business', 'Retired', 'Unemployed'
            setEmploymentType: (employmentType) => set({ employmentType }),
            residencyStatus: '', // 'Resident', 'NRI', 'OCI'
            setResidencyStatus: (residencyStatus) => set({ residencyStatus }),
            riskAnswers: {}, // { 1: score, 2: score, ... }
            setRiskAnswer: (qId, score) => set((state) => ({
                riskAnswers: { ...state.riskAnswers, [qId]: score }
            })),
            riskTolerance: '', // computed: 'conservative', 'moderate', 'aggressive'
            setRiskTolerance: (riskTolerance) => set({ riskTolerance }),

            // Step 2: Income & Expenses
            incomes: [], // { id, source, amount, frequency }
            addIncome: (income) => set((state) => ({ incomes: [...state.incomes, income] })),
            removeIncome: (id) => set((state) => ({ incomes: state.incomes.filter((i) => i.id !== id) })),
            updateIncome: (id, updates) => set((state) => ({ incomes: state.incomes.map((i) => (i.id === id ? { ...i, ...updates } : i)) })),

            expenses: [], // { id, category, amount, frequency, type }
            addExpense: (expense) => set((state) => ({ expenses: [...state.expenses, expense] })),
            removeExpense: (id) => set((state) => ({ expenses: state.expenses.filter((e) => e.id !== id) })),
            updateExpense: (id, updates) => set((state) => ({ expenses: state.expenses.map((e) => (e.id === id ? { ...e, ...updates } : e)) })),

            // Step 3: Assets & Liabilities
            assets: [], // { id, category, name, amount }
            addAsset: (asset) => set((state) => ({ assets: [...state.assets, asset] })),
            removeAsset: (id) => set((state) => ({ assets: state.assets.filter((a) => a.id !== id) })),

            liabilities: [], // { id, category, name, amount }
            addLiability: (liability) => set((state) => ({ liabilities: [...state.liabilities, liability] })),
            removeLiability: (id) => set((state) => ({ liabilities: state.liabilities.filter((l) => l.id !== id) })),

            // Step 4: Financial Goals
            goals: [], // { id, type, name, cost, horizon, currentSavings, inflation, importance }
            addGoal: (goal) => set((state) => ({ goals: [...state.goals, goal] })),
            removeGoal: (id) => set((state) => ({ goals: state.goals.filter((g) => g.id !== id) })),
            updateGoal: (id, updates) => set((state) => ({
                goals: state.goals.map((g) => (g.id === id ? { ...g, ...updates } : g)),
            })),

            // Step 5: Insurance Gap
            insurance: {
                corporateHealth: '',
                corporateHealthMembers: '',
                corporateLife: '',
                personalHealth: [], // { id, type, sumInsured, premium, copay }
                personalLife: [],   // { id, type, sumAssured, premium, spouseAge }
                checklist: {
                    criticalIllness: false,
                    personalAccident: false,
                    disability: false,
                    maternity: false,
                }
            },
            updateInsurance: (updates) => set((state) => {
                const ins = state.insurance || { corporateHealth: '', corporateHealthMembers: '', corporateLife: '', personalHealth: [], personalLife: [], checklist: { criticalIllness: false, personalAccident: false, disability: false, maternity: false } };
                return { insurance: { ...ins, ...updates } };
            }),
            addPersonalHealth: (policy) => set((state) => {
                const ins = state.insurance || { corporateHealth: '', corporateHealthMembers: '', corporateLife: '', personalHealth: [], personalLife: [], checklist: { criticalIllness: false, personalAccident: false, disability: false, maternity: false } };
                return { insurance: { ...ins, personalHealth: [...(ins.personalHealth || []), policy] } };
            }),
            removePersonalHealth: (id) => set((state) => {
                const ins = state.insurance || { corporateHealth: '', corporateHealthMembers: '', corporateLife: '', personalHealth: [], personalLife: [], checklist: { criticalIllness: false, personalAccident: false, disability: false, maternity: false } };
                return { insurance: { ...ins, personalHealth: (ins.personalHealth || []).filter(p => p.id !== id) } };
            }),
            addPersonalLife: (policy) => set((state) => {
                const ins = state.insurance || { corporateHealth: '', corporateHealthMembers: '', corporateLife: '', personalHealth: [], personalLife: [], checklist: { criticalIllness: false, personalAccident: false, disability: false, maternity: false } };
                return { insurance: { ...ins, personalLife: [...(ins.personalLife || []), policy] } };
            }),
            removePersonalLife: (id) => set((state) => {
                const ins = state.insurance || { corporateHealth: '', corporateHealthMembers: '', corporateLife: '', personalHealth: [], personalLife: [], checklist: { criticalIllness: false, personalAccident: false, disability: false, maternity: false } };
                return { insurance: { ...ins, personalLife: (ins.personalLife || []).filter(p => p.id !== id) } };
            }),
            toggleChecklist: (key) => set((state) => {
                const ins = state.insurance || { corporateHealth: '', corporateHealthMembers: '', corporateLife: '', personalHealth: [], personalLife: [], checklist: { criticalIllness: false, personalAccident: false, disability: false, maternity: false } };
                const checklist = ins.checklist || { criticalIllness: false, personalAccident: false, disability: false, maternity: false };
                return { insurance: { ...ins, checklist: { ...checklist, [key]: !checklist[key] } } };
            }),

            // Step 6: Tax Optimization
            taxRegime: 'new', // 'old', 'new'
            setTaxRegime: (taxRegime) => set({ taxRegime }),
            investments80C: 0,
            setInvestments80C: (amount) => set({ investments80C: amount }),

            // Navigation / Completion
            currentStep: 0,
            setCurrentStep: (step) => set({ currentStep: step }),
            isComplete: false,
            completeAssessment: () => set({ isComplete: true }),
        }),
        {
            name: 'assessment-storage', // unique name
        }
    )
);
