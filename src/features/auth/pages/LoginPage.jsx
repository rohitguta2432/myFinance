import { GoogleLogin } from '@react-oauth/google';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useAssessmentStore } from '../../assessment/store/useAssessmentStore';
import { api } from '../../../services/api';
import {
    ShieldCheck,
    BarChart3,
    TrendingUp,
    Target,
    PieChart,
    Users,
    Zap,
    Lock,
} from 'lucide-react';

/* ── stat cards data ── */
const STATS = [
    { label: 'SAVINGS', value: '₹12.5L+', sub: 'Tracked monthly', icon: TrendingUp },
    { label: 'PLANS', value: '2,400+', sub: 'Built this year', icon: Users },
    { label: 'TAX SAVED', value: '₹3.2L', sub: 'Average per user', icon: PieChart },
    { label: 'PRECISION', value: '98%', sub: 'Tax accuracy', icon: Zap },
];

const FEATURES = [
    { icon: BarChart3, title: 'Post-tax return analysis', desc: 'See what you actually keep after Uncle Sam.' },
    { icon: ShieldCheck, title: 'Tax regime optimizer', desc: 'Old vs New regime automatically compared.' },
    { icon: Target, title: 'Goal-based roadmap', desc: 'Visualize your journey to financial freedom.' },
];

export default function LoginPage() {
    const navigate = useNavigate();
    const login = useAuthStore((s) => s.login);

    const handleGoogleSuccess = async (credentialResponse) => {
        const credential = credentialResponse.credential;

        try {
            const res = await api.post('/auth/google', { credential });
            // res = { token: "jwt...", user: { id, email, name, pictureUrl } }
            login(res);
            // New users (no assessment) go straight to Step 1; returning users go to dashboard
            const hasAssessment = useAssessmentStore.getState().isComplete;
            navigate(hasAssessment ? '/' : '/assessment/step-1', { replace: true });
        } catch (err) {
            console.error('Authentication failed:', err);
        }
    };

    return (
        <>
            <div className="lp-page">
                {/* ── Top Navbar ── */}
                <nav className="lp-nav">
                    <div className="lp-nav-brand"><img src="/myfinancial-logo.svg" alt="MyFinancial" style={{ height: '40px' }} /></div>
                    <div className="lp-nav-links">
                        <a href="https://www.myfinancial.in/blog" target="_blank" rel="noopener noreferrer" className="font-bold">Blog</a>
                    </div>
                </nav>

                {/* ── Main Split ── */}
                <div className="lp-main">
                    {/* LEFT — Hero */}
                    <div className="lp-left">
                        <div className="lp-left-glow lp-left-glow--1" />
                        <div className="lp-left-glow lp-left-glow--2" />

                        <div className="lp-left-inner">
                            <div className="lp-hero">
                                <h1 className="lp-hero-title">
                                    Your finances,
                                    <br />
                                    <span className="lp-hero-green">simplified.</span>
                                </h1>
                                <p className="lp-hero-sub">
                                    Groww helps you invest. INDmoney tracks your money.
                                    MyFinancial tells you if you're financially healthy — and exactly what to fix.
                                </p>
                            </div>

                            {/* Stat cards row */}
                            <div className="lp-stats">
                                {STATS.map((s) => (
                                    <div className="lp-stat" key={s.label}>
                                        <span className="lp-stat-label">{s.label}</span>
                                        <span className="lp-stat-value">{s.value}</span>
                                        <span className="lp-stat-sub">{s.sub}</span>
                                    </div>
                                ))}
                            </div>
                        </div>
                    </div>

                    {/* RIGHT — Login Card */}
                    <div className="lp-right">
                        <div className="lp-card">
                            <h2 className="lp-card-title">Welcome back</h2>
                            <p className="lp-card-sub">Continue to your financial dashboard</p>

                            {/* Google Sign-In (official component — returns ID token) */}
                            <div className="lp-google-wrap">
                                <GoogleLogin
                                    onSuccess={handleGoogleSuccess}
                                    onError={() => console.error('Google login error')}
                                    theme="filled_black"
                                    size="large"
                                    width="340"
                                    text="signin_with"
                                    shape="rectangular"
                                />
                            </div>


                            {/* Feature highlights */}
                            <div className="lp-features">
                                {FEATURES.map(({ icon: Icon, title, desc }) => (
                                    <div className="lp-feature" key={title}>
                                        <div className="lp-feature-icon">
                                            <Icon size={16} />
                                        </div>
                                        <div>
                                            <div className="lp-feature-title">{title}</div>
                                            <div className="lp-feature-desc">{desc}</div>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* Privacy */}
                            <p className="lp-privacy">
                                <Lock size={12} style={{ marginRight: 4, opacity: 0.5 }} />
                                Your data is encrypted with 256-bit SSL. By signing in, you agree to our{' '}
                                <a href="#terms">Terms</a> and <a href="#privacy">Privacy Policy</a>.
                            </p>
                        </div>
                    </div>
                </div>

                {/* ── Footer ── */}
                <footer className="lp-footer">
                    <span className="lp-footer-brand">MyFinancial</span>
                    <div className="lp-footer-links">
                        <a href="#privacy">Privacy Policy</a>
                        <a href="#terms">Terms of Service</a>
                        <a href="#security">Security</a>
                        <a href="#cookies">Cookie Settings</a>
                    </div>
                    <span className="lp-footer-copy">© 2024 MyFinancial Technologies Private Limited</span>
                </footer>
            </div>

            {/* ── Scoped styles ── */}
            <style>{`
                /* ── Reset ── */
                .lp-page {
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    background: #060b11;
                    color: #fff;
                    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
                    overflow-x: hidden;
                }

                /* ── NAVBAR ── */
                .lp-nav {
                    display: flex;
                    align-items: center;
                    padding: 0 40px;
                    height: 60px;
                    border-bottom: 1px solid rgba(255,255,255,0.06);
                    flex-shrink: 0;
                    z-index: 10;
                }
                .lp-nav-brand {
                    font-size: 1.2rem;
                    font-weight: 700;
                    color: #0DF259;
                    letter-spacing: -0.02em;
                }
                .lp-nav-links {
                    margin-left: auto;
                    display: flex;
                    gap: 32px;
                }
                .lp-nav-links a {
                    color: rgba(255,255,255,0.6);
                    text-decoration: none;
                    font-size: 0.875rem;
                    font-weight: 500;
                    transition: color 0.2s;
                }
                .lp-nav-links a:hover { color: #fff; }
                .lp-nav-signup {
                    margin-left: 32px;
                    background: #0DF259;
                    color: #060b11;
                    border: none;
                    padding: 8px 20px;
                    border-radius: 8px;
                    font-weight: 600;
                    font-size: 0.875rem;
                    cursor: pointer;
                    transition: background 0.2s;
                }
                .lp-nav-signup:hover { background: #0bda4f; }

                /* ── MAIN SPLIT ── */
                .lp-main {
                    flex: 1;
                    display: flex;
                    min-height: 0;
                }

                /* LEFT */
                .lp-left {
                    flex: 1.4;
                    position: relative;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    padding: 60px 64px;
                    overflow: hidden;
                    background: linear-gradient(160deg, #060b11 0%, #071410 50%, #060b11 100%);
                }
                .lp-left-glow {
                    position: absolute;
                    border-radius: 50%;
                    filter: blur(120px);
                    opacity: 0.15;
                    pointer-events: none;
                }
                .lp-left-glow--1 {
                    width: 500px; height: 500px;
                    background: #0DF259;
                    top: -100px; left: -100px;
                }
                .lp-left-glow--2 {
                    width: 400px; height: 400px;
                    background: #10B981;
                    bottom: -50px; right: 50px;
                }
                .lp-left-inner {
                    position: relative;
                    z-index: 2;
                    max-width: 680px;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    flex: 1;
                }
                .lp-hero {
                    margin-bottom: auto;
                    padding-top: 40px;
                }

                .lp-hero-title {
                    font-size: clamp(2.8rem, 4.5vw, 4.2rem);
                    font-weight: 800;
                    line-height: 1.05;
                    letter-spacing: -0.03em;
                    color: #fff;
                    margin: 0 0 20px;
                }
                .lp-hero-green {
                    color: #0DF259;
                }
                .lp-hero-sub {
                    color: rgba(255,255,255,0.5);
                    font-size: 1.125rem;
                    line-height: 1.6;
                    max-width: 460px;
                    margin: 0;
                }

                /* Stat row */
                .lp-stats {
                    display: grid;
                    grid-template-columns: repeat(4, 1fr);
                    gap: 16px;
                    margin-top: auto;
                    padding-bottom: 8px;
                }
                .lp-stat {
                    background: rgba(255,255,255,0.04);
                    border: 1px solid rgba(255,255,255,0.07);
                    border-radius: 16px;
                    padding: 20px 18px;
                    display: flex;
                    flex-direction: column;
                    gap: 6px;
                    backdrop-filter: blur(12px);
                    transition: transform 0.25s, border-color 0.25s;
                }
                .lp-stat:hover {
                    transform: translateY(-3px);
                    border-color: rgba(13,242,89,0.25);
                }
                .lp-stat-label {
                    font-size: 0.65rem;
                    font-weight: 700;
                    letter-spacing: 0.08em;
                    color: #0DF259;
                    text-transform: uppercase;
                }
                .lp-stat-value {
                    font-size: 1.5rem;
                    font-weight: 800;
                    color: #fff;
                    letter-spacing: -0.02em;
                }
                .lp-stat-sub {
                    font-size: 0.75rem;
                    color: rgba(255,255,255,0.35);
                }

                /* RIGHT */
                .lp-right {
                    flex: 0.6;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 40px 48px;
                    background: #0a1018;
                    border-left: 1px solid rgba(255,255,255,0.06);
                }

                /* Card */
                .lp-card {
                    width: 100%;
                    max-width: 400px;
                }
                .lp-card-title {
                    font-size: 1.65rem;
                    font-weight: 700;
                    margin: 0 0 6px;
                    text-align: center;
                    letter-spacing: -0.02em;
                }
                .lp-card-sub {
                    text-align: center;
                    color: rgba(255,255,255,0.45);
                    font-size: 0.875rem;
                    margin: 0 0 28px;
                }

                /* Google button wrapper */
                .lp-google-wrap {
                    display: flex;
                    justify-content: center;
                    margin-bottom: 4px;
                }
                .lp-google-icon { flex-shrink: 0; }

                /* Divider */
                .lp-divider {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    margin: 24px 0;
                }
                .lp-divider::before,
                .lp-divider::after {
                    content: '';
                    flex: 1;
                    height: 1px;
                    background: rgba(255,255,255,0.08);
                }
                .lp-divider span {
                    font-size: 0.7rem;
                    font-weight: 600;
                    letter-spacing: 0.1em;
                    color: rgba(255,255,255,0.3);
                }

                /* Email field */
                .lp-field-label {
                    display: block;
                    font-size: 0.7rem;
                    font-weight: 600;
                    letter-spacing: 0.08em;
                    color: rgba(255,255,255,0.45);
                    margin-bottom: 8px;
                }
                .lp-field-input {
                    width: 100%;
                    padding: 12px 16px;
                    border: 1px solid rgba(255,255,255,0.1);
                    border-radius: 10px;
                    background: rgba(255,255,255,0.04);
                    color: #fff;
                    font-size: 0.9375rem;
                    outline: none;
                    transition: border-color 0.2s;
                    box-sizing: border-box;
                }
                .lp-field-input::placeholder { color: rgba(255,255,255,0.25); }
                .lp-field-input:focus { border-color: rgba(13,242,89,0.4); }

                .lp-email-btn {
                    width: 100%;
                    margin-top: 14px;
                    padding: 13px 0;
                    border: none;
                    border-radius: 10px;
                    background: rgba(255,255,255,0.08);
                    color: rgba(255,255,255,0.6);
                    font-size: 0.9375rem;
                    font-weight: 600;
                    cursor: pointer;
                    transition: background 0.2s, color 0.2s;
                }
                .lp-email-btn:hover {
                    background: rgba(255,255,255,0.12);
                    color: #fff;
                }

                /* Features */
                .lp-features {
                    margin-top: 32px;
                    display: flex;
                    flex-direction: column;
                    gap: 16px;
                }
                .lp-feature {
                    display: flex;
                    align-items: flex-start;
                    gap: 12px;
                }
                .lp-feature-icon {
                    width: 32px;
                    height: 32px;
                    border-radius: 8px;
                    background: rgba(13,242,89,0.12);
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #0DF259;
                    flex-shrink: 0;
                }
                .lp-feature-title {
                    font-size: 0.875rem;
                    font-weight: 600;
                    color: #fff;
                    margin-bottom: 2px;
                }
                .lp-feature-desc {
                    font-size: 0.78rem;
                    color: rgba(255,255,255,0.4);
                    line-height: 1.4;
                }

                /* Privacy */
                .lp-privacy {
                    margin-top: 28px;
                    font-size: 0.72rem;
                    color: rgba(255,255,255,0.3);
                    text-align: center;
                    line-height: 1.5;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex-wrap: wrap;
                    gap: 2px;
                }
                .lp-privacy a {
                    color: #0DF259;
                    text-decoration: underline;
                    text-underline-offset: 2px;
                }

                /* ── FOOTER ── */
                .lp-footer {
                    display: flex;
                    align-items: center;
                    padding: 16px 40px;
                    border-top: 1px solid rgba(255,255,255,0.06);
                    flex-shrink: 0;
                    gap: 24px;
                }
                .lp-footer-brand {
                    font-weight: 700;
                    font-size: 0.85rem;
                    color: rgba(255,255,255,0.5);
                }
                .lp-footer-links {
                    display: flex;
                    gap: 20px;
                    margin-left: auto;
                }
                .lp-footer-links a {
                    color: rgba(255,255,255,0.35);
                    text-decoration: none;
                    font-size: 0.78rem;
                    transition: color 0.2s;
                }
                .lp-footer-links a:hover { color: rgba(255,255,255,0.6); }
                .lp-footer-copy {
                    font-size: 0.75rem;
                    color: rgba(255,255,255,0.25);
                    margin-left: 24px;
                }

                /* ── Responsive ── */
                @media (max-width: 900px) {
                    .lp-main { flex-direction: column; }
                    .lp-left { padding: 40px 24px; }
                    .lp-right {
                        border-left: none;
                        border-top: 1px solid rgba(255,255,255,0.06);
                        padding: 40px 24px;
                    }
                    .lp-stats {
                        grid-template-columns: repeat(2, 1fr);
                    }
                    .lp-nav-links { display: none; }
                    .lp-footer { flex-wrap: wrap; gap: 12px; }
                    .lp-footer-links { margin-left: 0; }
                    .lp-footer-copy { margin-left: 0; }
                }
            `}</style>
        </>
    );
}
