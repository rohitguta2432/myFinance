-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    provider VARCHAR(50) DEFAULT 'email',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Profiles Table (Step 1)
CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    age INTEGER CHECK (age >= 18),
    city_tier VARCHAR(20) CHECK (city_tier IN ('METRO', 'TIER_1', 'TIER_2', 'TIER_3')),
    marital_status VARCHAR(20) CHECK (marital_status IN ('SINGLE', 'MARRIED')),
    dependents INTEGER DEFAULT 0,
    employment_type VARCHAR(50),
    residency_status VARCHAR(50),
    risk_score DECIMAL(4, 2), -- 0.00 to 10.00
    risk_tolerance VARCHAR(20) CHECK (risk_tolerance IN ('CONSERVATIVE', 'MODERATE', 'AGGRESSIVE')),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE profiles ADD CONSTRAINT uq_profiles_user_id UNIQUE (user_id);

-- Incomes Table (Step 2)
CREATE TABLE incomes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    source_name VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    frequency VARCHAR(20) DEFAULT 'MONTHLY' CHECK (frequency IN ('MONTHLY', 'YEARLY', 'ONE_TIME')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Expenses Table (Step 2)
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    frequency VARCHAR(20) DEFAULT 'MONTHLY',
    is_essential BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Assets Table (Step 3)
CREATE TABLE assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    asset_type VARCHAR(50) NOT NULL, -- REAL_ESTATE, EQUITY, GOLD, DEBT
    name VARCHAR(100),
    current_value DECIMAL(15, 2) NOT NULL,
    allocation_percentage DECIMAL(5, 2), -- Derived or stored
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Liabilities Table (Step 3)
CREATE TABLE liabilities (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    liability_type VARCHAR(50) NOT NULL, -- HOME_LOAN, CAR_LOAN, etc.
    name VARCHAR(100),
    outstanding_amount DECIMAL(15, 2) NOT NULL,
    monthly_emi DECIMAL(15, 2),
    interest_rate DECIMAL(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Financial Goals Table (Step 4)
CREATE TABLE financial_goals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    goal_type VARCHAR(50) NOT NULL, -- HOME, CAR, RETIREMENT, EDUCATION
    name VARCHAR(100),
    target_amount DECIMAL(15, 2) NOT NULL,
    current_cost DECIMAL(15, 2),
    time_horizon_years INTEGER NOT NULL,
    inflation_rate DECIMAL(4, 2) DEFAULT 6.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insurance Coverage Table (Step 5)
CREATE TABLE insurances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    insurance_type VARCHAR(50) NOT NULL CHECK (insurance_type IN ('LIFE', 'HEALTH')),
    policy_name VARCHAR(100),
    coverage_amount DECIMAL(15, 2) NOT NULL,
    premium_amount DECIMAL(15, 2),
    renewal_date DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Tax Planning Table (Step 6)
CREATE TABLE tax_planning (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    
    -- 80C Components
    epf_vpf_amount DECIMAL(15, 2) DEFAULT 0,
    ppf_elss_amount DECIMAL(15, 2) DEFAULT 0,
    tuition_fees_amount DECIMAL(15, 2) DEFAULT 0,
    lic_premium_amount DECIMAL(15, 2) DEFAULT 0,
    home_loan_principal DECIMAL(15, 2) DEFAULT 0,
    
    -- 80D
    health_insurance_premium DECIMAL(15, 2) DEFAULT 0,
    parents_health_insurance DECIMAL(15, 2) DEFAULT 0,
    
    selected_regime VARCHAR(10) CHECK (selected_regime IN ('OLD', 'NEW')),
    calculated_tax_old DECIMAL(15, 2),
    calculated_tax_new DECIMAL(15, 2),
    
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE tax_planning ADD CONSTRAINT uq_tax_planning_user_id UNIQUE (user_id);
