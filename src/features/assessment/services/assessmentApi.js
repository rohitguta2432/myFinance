import { api } from '../../../services/api';

// ─── Enum Conversion Maps ───────────────────────────────────────────────────
// Frontend display values ↔ Backend UPPERCASE DB enum values

const MARITAL_TO_DB = { 'single': 'SINGLE', 'married': 'MARRIED', 'divorced': 'DIVORCED', 'widowed': 'WIDOWED' };

const MARITAL_FROM_DB = Object.fromEntries(Object.entries(MARITAL_TO_DB).map(([k, v]) => [v, k]));

const RISK_TO_DB = { 'low': 'CONSERVATIVE', 'medium': 'MODERATE', 'high': 'AGGRESSIVE' };
const RISK_FROM_DB = Object.fromEntries(Object.entries(RISK_TO_DB).map(([k, v]) => [v, k]));

const FREQ_TO_DB = { 'Monthly': 'MONTHLY', 'Yearly': 'YEARLY', 'One-time': 'ONE_TIME' };
const FREQ_FROM_DB = Object.fromEntries(Object.entries(FREQ_TO_DB).map(([k, v]) => [v, k]));

const INSURANCE_TO_DB = { 'life': 'LIFE', 'health': 'HEALTH' };

const REGIME_TO_DB = { 'old': 'OLD', 'new': 'NEW' };
const REGIME_FROM_DB = { 'OLD': 'old', 'NEW': 'new' };

// ─── Field Mappers ──────────────────────────────────────────────────────────
// Frontend Zustand shapes ↔ Backend DTO shapes

const mapProfileToDTO = (data) => ({
    age: data.age ? parseInt(data.age) : null,
    city: data.city || null,
    maritalStatus: MARITAL_TO_DB[data.maritalStatus] || data.maritalStatus?.toUpperCase(),
    dependents: parseInt(data.dependents) || 0,
    childDependents: parseInt(data.childDependents) || 0,
    employmentType: data.employmentType?.toUpperCase().replace(/ /g, '_'),
    residencyStatus: data.residencyStatus?.toUpperCase(),
    riskScore: null, // Calculated server-side
    riskTolerance: RISK_TO_DB[data.riskTolerance] || data.riskTolerance?.toUpperCase(),
    riskAnswers: data.riskAnswers || {},
});

const mapProfileFromDTO = (dto) => ({
    age: dto.age ?? '',
    city: dto.city ?? '',
    maritalStatus: MARITAL_FROM_DB[dto.maritalStatus] ?? dto.maritalStatus?.toLowerCase() ?? '',
    dependents: dto.dependents ?? 0,
    childDependents: dto.childDependents ?? 0,
    employmentType: dto.employmentType
        ? dto.employmentType.charAt(0).toUpperCase() + dto.employmentType.slice(1).toLowerCase().replace(/_/g, ' ')
        : '',
    residencyStatus: dto.residencyStatus
        ? dto.residencyStatus.charAt(0).toUpperCase() + dto.residencyStatus.slice(1).toLowerCase()
        : '',
    riskTolerance: RISK_FROM_DB[dto.riskTolerance] ?? dto.riskTolerance?.toLowerCase() ?? '',
    riskAnswers: dto.riskAnswers ?? {},
});

const mapIncomeToDTO = (data) => ({
    sourceName: data.source,
    amount: parseFloat(data.amount) || 0,
    frequency: FREQ_TO_DB[data.frequency] || data.frequency?.toUpperCase(),
});

const mapIncomeFromDTO = (dto) => ({
    id: dto.id,
    source: dto.sourceName,
    amount: dto.amount,
    frequency: FREQ_FROM_DB[dto.frequency] ?? dto.frequency ?? 'Monthly',
});

const mapExpenseToDTO = (data) => ({
    category: data.category,
    amount: parseFloat(data.amount) || 0,
    frequency: FREQ_TO_DB[data.frequency] || data.frequency?.toUpperCase(),
    isEssential: data.type === 'essential',
});

const mapExpenseFromDTO = (dto) => ({
    id: dto.id,
    category: dto.category,
    amount: dto.amount,
    frequency: FREQ_FROM_DB[dto.frequency] ?? dto.frequency ?? 'Monthly',
    type: dto.isEssential ? 'essential' : 'discretionary',
});

const mapAssetToDTO = (data) => ({
    assetType: data.category,
    name: data.name,
    currentValue: parseFloat(data.amount) || 0,
    allocationPercentage: null, // Calculated server-side
});

const mapAssetFromDTO = (dto) => ({
    id: dto.id,
    category: dto.assetType,
    name: dto.name,
    amount: dto.currentValue,
});

const mapLiabilityToDTO = (data) => ({
    liabilityType: data.category,
    name: data.name,
    outstandingAmount: parseFloat(data.amount) || 0,
    monthlyEmi: null,
    interestRate: null,
});

const mapLiabilityFromDTO = (dto) => ({
    id: dto.id,
    category: dto.liabilityType,
    name: dto.name,
    amount: dto.outstandingAmount,
});

const mapGoalToDTO = (data) => ({
    goalType: data.type,
    name: data.name,
    targetAmount: null, // Calculated from currentCost + inflation
    currentCost: parseFloat(data.cost) || 0,
    timeHorizonYears: parseInt(data.horizon) || 5,
    inflationRate: data.inflation ? parseFloat(data.inflation) / 100 : 0.06,
});

const mapGoalFromDTO = (dto) => ({
    id: dto.id,
    type: dto.goalType,
    name: dto.name,
    cost: dto.currentCost,
    horizon: dto.timeHorizonYears,
    inflation: dto.inflationRate ? dto.inflationRate * 100 : 6,
});

const mapInsuranceToDTO = (type, data) => ({
    insuranceType: INSURANCE_TO_DB[type] || type.toUpperCase(),
    policyName: `${type.charAt(0).toUpperCase() + type.slice(1)} Insurance`,
    coverageAmount: parseFloat(data) || 0,
    premiumAmount: null,
    renewalDate: null,
});

const mapInsuranceFromDTO = (dtoList) => {
    const result = { life: 0, health: 0 };
    if (!Array.isArray(dtoList)) return result;
    dtoList.forEach((dto) => {
        const key = dto.insuranceType?.toLowerCase();
        if (key === 'life') result.life = dto.coverageAmount || 0;
        if (key === 'health') result.health = dto.coverageAmount || 0;
    });
    return result;
};

const mapTaxToDTO = (data) => ({
    selectedRegime: REGIME_TO_DB[data.taxRegime] || data.taxRegime?.toUpperCase(),
    ppfElssAmount: parseFloat(data.investments80C) || 0,
    epfVpfAmount: null,
    tuitionFeesAmount: null,
    licPremiumAmount: null,
    homeLoanPrincipal: null,
    healthInsurancePremium: null,
    parentsHealthInsurance: null,
    calculatedTaxOld: null,
    calculatedTaxNew: null,
});

const mapTaxFromDTO = (dto) => ({
    taxRegime: REGIME_FROM_DB[dto.selectedRegime] ?? dto.selectedRegime?.toLowerCase() ?? 'new',
    investments80C: dto.ppfElssAmount ?? 0,
});

// ─── API Functions ──────────────────────────────────────────────────────────

// Step 1: Profile
export const getProfile = async () => {
    const dto = await api.get('/profile');
    return mapProfileFromDTO(dto);
};

export const saveProfile = async (data) => {
    const dto = await api.post('/profile', mapProfileToDTO(data));
    return mapProfileFromDTO(dto);
};

// Step 2: Financials
export const getFinancials = async () => {
    const dto = await api.get('/financials');
    return {
        incomes: (dto.incomes || []).map(mapIncomeFromDTO),
        expenses: (dto.expenses || []).map(mapExpenseFromDTO),
    };
};

export const addIncome = async (data) => {
    const dto = await api.post('/income', mapIncomeToDTO(data));
    return mapIncomeFromDTO(dto);
};

export const addExpense = async (data) => {
    const dto = await api.post('/expense', mapExpenseToDTO(data));
    return mapExpenseFromDTO(dto);
};

// Step 3: Balance Sheet
export const getBalanceSheet = async () => {
    const dto = await api.get('/balance-sheet');
    return {
        assets: (dto.assets || []).map(mapAssetFromDTO),
        liabilities: (dto.liabilities || []).map(mapLiabilityFromDTO),
    };
};

export const addAsset = async (data) => {
    const dto = await api.post('/asset', mapAssetToDTO(data));
    return mapAssetFromDTO(dto);
};

export const addLiability = async (data) => {
    const dto = await api.post('/liability', mapLiabilityToDTO(data));
    return mapLiabilityFromDTO(dto);
};

// Step 4: Goals
export const getGoals = async () => {
    const dtoList = await api.get('/goals');
    return (dtoList || []).map(mapGoalFromDTO);
};

export const addGoal = async (data) => {
    const dto = await api.post('/goal', mapGoalToDTO(data));
    return mapGoalFromDTO(dto);
};

// Step 5: Insurance
export const getInsurance = async () => {
    const dtoList = await api.get('/insurance');
    return mapInsuranceFromDTO(dtoList);
};

export const saveInsurance = async (insurance) => {
    // Send each type as a separate policy
    const results = await Promise.all([
        api.post('/insurance', mapInsuranceToDTO('life', insurance.life)),
        api.post('/insurance', mapInsuranceToDTO('health', insurance.health)),
    ]);
    return mapInsuranceFromDTO(results);
};

// Step 6: Tax
export const getTax = async () => {
    const dto = await api.get('/tax');
    return mapTaxFromDTO(dto);
};

export const saveTax = async (data) => {
    const dto = await api.post('/tax', mapTaxToDTO(data));
    return mapTaxFromDTO(dto);
};
