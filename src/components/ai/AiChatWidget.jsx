import { useState, useRef, useEffect } from 'react';
import { api } from '../../services/api';

// ─── QUICK SUGGESTION CHIPS ─────────────────────────
const QUICK_SUGGESTIONS = [
  "What's my savings rate?",
  "How can I save on taxes?",
  "How much emergency fund do I need?",
  "Am I on track for retirement?",
  "Is my insurance cover enough?",
];

// ─── FAQ DATA ────────────────────────────────────────
const FAQ_ITEMS = [
  {
    q: "What is MyFinancial?",
    a: "MyFinancial is a free personal financial planning tool that helps you understand your finances, optimize taxes, and plan for the future — all based on your unique profile."
  },
  {
    q: "Is my data safe?",
    a: "Absolutely! Your financial data is stored locally on your device and is never shared with third parties. We use bank-grade encryption for any data transmission."
  },
  {
    q: "How does tax optimization work?",
    a: "We compare Old vs New tax regimes based on your income, deductions (80C, 80D, 80CCD, HRA), and suggest the one that saves you the most. We also recommend investment strategies to maximize tax savings."
  },
  {
    q: "What is an emergency fund?",
    a: "An emergency fund is liquid savings covering 3-6 months of expenses. It protects you from unexpected costs like medical bills, job loss, or car repairs without touching your investments."
  },
  {
    q: "How much life insurance do I need?",
    a: "A good rule of thumb is 10-15x your annual income. We calculate your exact gap based on your income, loans, dependents, and existing coverage."
  },
  {
    q: "What is asset allocation?",
    a: "It's the strategy of splitting your investments across equity, debt, gold, and real estate to balance risk and returns based on your age, goals, and risk tolerance."
  },
  {
    q: "Can Kira give specific stock tips?",
    a: "No — Kira provides general financial guidance, not specific stock or mutual fund recommendations. For personalized investment advice, consult a SEBI-registered financial advisor."
  },
];

// ─── API CALL ────────────────────────────────────────
async function sendMessage(message, history, financialContext) {
  return api.post('/chat', { message, history, financialContext });
}

// ─── BUILD FINANCIAL CONTEXT FROM USER DATA ──────────
function buildFinancialContext(user) {
  if (!user) return {};
  return {
    name: user.name,
    age: user.age,
    city: user.city,
    grossIncome: `₹${(user.grossIncome || 0).toLocaleString('en-IN')}/year`,
    monthlyTakeHome: `₹${(user.takeHomePay || 0).toLocaleString('en-IN')}`,
    monthlyExpenses: `₹${(user.monthlyExpenses || 0).toLocaleString('en-IN')}`,
    monthlySurplus: `₹${(user.monthlySurplus || 0).toLocaleString('en-IN')}`,
    monthlyEMI: `₹${(user.monthlyEMI || 0).toLocaleString('en-IN')}`,
    liquidSavings: `₹${(user.liquidAssets || 0).toLocaleString('en-IN')}`,
    emergencyFundMonths: user.liquidMonths,
    totalInvestments: `₹${(user.totalFinancialAssets || 0).toLocaleString('en-IN')}`,
    netWorth: `₹${(user.netWorth || 0).toLocaleString('en-IN')}`,
    equityAllocation: `${user.equityPct}%`,
    targetEquity: `${user.targetEquityPct}%`,
    existingLifeCover: `₹${(user.existingTermCover || 0).toLocaleString('en-IN')}`,
    requiredLifeCover: `₹${(user.requiredCover || 0).toLocaleString('en-IN')}`,
    healthCover: `₹${(user.existingHealthCover || 0).toLocaleString('en-IN')}`,
    recommendedHealthCover: `₹${(user.cityHealthBenchmark || 0).toLocaleString('en-IN')}`,
    invested80C: `₹${(user.invested80C || 0).toLocaleString('en-IN')}`,
    npsContribution: `₹${(user.npsContribution || 0).toLocaleString('en-IN')}`,
    taxRegime: user.taxRegime,
    currentSIP: `₹${(user.currentSIP || 0).toLocaleString('en-IN')}/month`,
    emiToIncomeRatio: `${user.emiRatio}%`,
    debtToIncome: `${user.dti}%`,
    ownsHome: user.ownsHome ? 'Yes' : 'No',
    currentRent: `₹${(user.currentRent || 0).toLocaleString('en-IN')}/month`,
    retirementAge: user.retirementAge,
  };
}

// ─── DETECT THEME ────────────────────────────────────
function useAppTheme() {
  const [isDark, setIsDark] = useState(true);
  useEffect(() => {
    const check = () => {
      const t = document.documentElement.getAttribute('data-theme');
      setIsDark(t !== 'light');
    };
    check();
    const observer = new MutationObserver(check);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    return () => observer.disconnect();
  }, []);
  return isDark;
}

// ─── THEME TOKENS ────────────────────────────────────
const getTheme = (isDark) => ({
  panelBg: isDark ? 'rgba(15, 23, 42, 0.97)' : 'rgba(255, 255, 255, 0.97)',
  panelBorder: isDark ? 'rgba(51, 65, 85, 0.6)' : 'rgba(203, 213, 225, 0.8)',
  panelShadow: isDark
    ? '0 25px 60px rgba(0,0,0,.6), 0 0 40px rgba(13,148,136,.15)'
    : '0 25px 60px rgba(0,0,0,.15), 0 0 40px rgba(13,148,136,.08)',
  headerBg: isDark
    ? 'linear-gradient(135deg, rgba(13,148,136,.2), rgba(2,132,199,.15))'
    : 'linear-gradient(135deg, rgba(13,148,136,.08), rgba(2,132,199,.06))',
  headerBorder: isDark ? 'rgba(51,65,85,.5)' : 'rgba(203,213,225,.6)',
  titleColor: isDark ? '#F1F5F9' : '#0F172A',
  msgBotBg: isDark ? 'rgba(30, 41, 59, 0.8)' : 'rgba(241, 245, 249, 0.9)',
  msgBotBorder: isDark ? 'rgba(51,65,85,.5)' : 'rgba(203,213,225,.6)',
  msgBotColor: isDark ? '#CBD5E1' : '#334155',
  inputBg: isDark ? '#1E293B' : '#F8FAFC',
  inputBorder: isDark ? '#334155' : '#CBD5E1',
  inputColor: isDark ? '#F1F5F9' : '#0F172A',
  inputBarBg: isDark ? 'rgba(7,11,20,.5)' : 'rgba(248,250,252,.9)',
  inputBarBorder: isDark ? 'rgba(51,65,85,.4)' : 'rgba(203,213,225,.5)',
  sendDisabledBg: isDark ? '#1E293B' : '#E2E8F0',
  sendDisabledColor: isDark ? '#475569' : '#94A3B8',
  codeBg: isDark ? '#1E293B' : '#E2E8F0',
  tabBg: isDark ? 'rgba(30,41,59,.6)' : 'rgba(241,245,249,.8)',
  tabActiveBg: isDark ? 'rgba(13,148,136,.15)' : 'rgba(13,148,136,.1)',
  tabColor: isDark ? '#94A3B8' : '#64748B',
  tabActiveColor: '#0D9488',
  faqBg: isDark ? 'rgba(30,41,59,.5)' : 'rgba(241,245,249,.8)',
  faqBorder: isDark ? 'rgba(51,65,85,.4)' : 'rgba(203,213,225,.5)',
  faqQColor: isDark ? '#E2E8F0' : '#0F172A',
  faqAColor: isDark ? '#94A3B8' : '#475569',
  faqHoverBg: isDark ? 'rgba(13,148,136,.08)' : 'rgba(13,148,136,.05)',
});

// ─── CHAT WIDGET COMPONENT ──────────────────────────
export default function AiChatWidget({ user }) {
  const isDark = useAppTheme();
  const t = getTheme(isDark);
  const [isOpen, setIsOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('chat'); // 'chat' | 'faq'
  const [expandedFaq, setExpandedFaq] = useState(null);
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Hello! 👋 I\'m **Kira** — your personal financial advisor.\n\nAsk me anything about your finances. I\'ll give you personalized advice based on your financial profile.\n\nWhat would you like to know?',
    },
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(true);
  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  // Focus input when chat opens
  useEffect(() => {
    if (isOpen && activeTab === 'chat') setTimeout(() => inputRef.current?.focus(), 300);
  }, [isOpen, activeTab]);

  const handleSend = async (text) => {
    const msg = text || input.trim();
    if (!msg || isLoading) return;

    setInput('');
    setShowSuggestions(false);

    const userMsg = { role: 'user', content: msg };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    try {
      const history = [...messages, userMsg].slice(-10).map((m) => ({
        role: m.role,
        content: m.content,
      }));

      const financialContext = buildFinancialContext(user);
      const data = await sendMessage(msg, history, financialContext);

      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: data.reply },
      ]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content:
            'I couldn\'t process your request right now. Please try again in a moment.',
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // ─── SIMPLE MARKDOWN RENDERER ──────────────────────
  const renderMarkdown = (text) => {
    if (!text) return '';
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, `<code style="background:${t.codeBg};padding:1px 5px;border-radius:4px;font-size:11px">$1</code>`)
      .replace(/\n- /g, '\n• ')
      .replace(/\n/g, '<br/>');
  };

  return (
    <>
      <style>{`
        @keyframes meraFloat {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-4px); }
        }
        @keyframes meraPulse {
          0% { box-shadow: 0 0 0 0 rgba(13,148,136,.5); }
          70% { box-shadow: 0 0 0 14px rgba(13,148,136,0); }
          100% { box-shadow: 0 0 0 0 rgba(13,148,136,0); }
        }
        @keyframes meraSlideUp {
          from { opacity: 0; transform: translateY(20px) scale(.96); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
        @keyframes meraTyping {
          0%, 80%, 100% { transform: scale(0); opacity: .5; }
          40% { transform: scale(1); opacity: 1; }
        }
        .mera-msg-enter {
          animation: meraSlideUp .3s ease forwards;
        }
        .mera-faq-item {
          transition: background .2s;
        }
        .mera-scrollbar::-webkit-scrollbar {
          width: 4px;
        }
        .mera-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .mera-scrollbar::-webkit-scrollbar-thumb {
          background: rgba(13,148,136,.3);
          border-radius: 4px;
        }
      `}</style>

      {/* ─── FLOATING BUTTON ─────────────────────────── */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          aria-label="Open Kira AI Assistant"
          style={{
            position: 'fixed',
            bottom: 80,
            right: 24,
            width: 60,
            height: 60,
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #0D9488, #0284C7)',
            border: 'none',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 26,
            color: 'white',
            zIndex: 9999,
            animation: 'meraFloat 3s ease-in-out infinite, meraPulse 2s infinite',
            boxShadow: '0 8px 32px rgba(13,148,136,.45)',
            transition: 'transform .2s',
          }}
          onMouseEnter={(e) => (e.target.style.transform = 'scale(1.1)')}
          onMouseLeave={(e) => (e.target.style.transform = 'scale(1)')}
        >
          🧠
        </button>
      )}

      {/* ─── CHAT PANEL ──────────────────────────────── */}
      {isOpen && (
        <div
          style={{
            position: 'fixed',
            bottom: 80,
            right: 24,
            width: 380,
            height: 560,
            borderRadius: 20,
            background: t.panelBg,
            border: `1px solid ${t.panelBorder}`,
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            boxShadow: t.panelShadow,
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            zIndex: 9999,
            animation: 'meraSlideUp .35s cubic-bezier(.34,1.56,.64,1) forwards',
            fontFamily: "'DM Sans', sans-serif",
          }}
        >
          {/* ─── HEADER ────────────────────────────────── */}
          <div
            style={{
              padding: '14px 16px',
              background: t.headerBg,
              borderBottom: `1px solid ${t.headerBorder}`,
              display: 'flex',
              alignItems: 'center',
              gap: 11,
            }}
          >
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: 12,
                background: 'linear-gradient(135deg, #0D9488, #0284C7)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: 20,
                boxShadow: '0 4px 15px rgba(13,148,136,.4)',
              }}
            >
              🧠
            </div>
            <div style={{ flex: 1 }}>
              <div
                style={{
                  fontSize: 15,
                  fontWeight: 700,
                  color: t.titleColor,
                  fontFamily: 'Georgia, serif',
                }}
              >
                Kira
              </div>
              <div style={{ fontSize: 10, color: '#0D9488', display: 'flex', alignItems: 'center', gap: 4 }}>
                <span
                  style={{
                    width: 6,
                    height: 6,
                    borderRadius: '50%',
                    background: '#22C55E',
                    display: 'inline-block',
                  }}
                />
                Your Financial Advisor
              </div>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              aria-label="Close chat"
              style={{
                width: 30,
                height: 30,
                borderRadius: 8,
                background: 'rgba(239,68,68,.12)',
                border: '1px solid rgba(239,68,68,.25)',
                color: '#EF4444',
                fontSize: 14,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: 'sans-serif',
              }}
            >
              ✕
            </button>
          </div>

          {/* ─── TABS ─────────────────────────────────── */}
          <div
            style={{
              display: 'flex',
              padding: '6px 14px',
              gap: 6,
              borderBottom: `1px solid ${t.headerBorder}`,
            }}
          >
            {[
              { id: 'chat', label: '💬 Chat', },
              { id: 'faq', label: '❓ FAQ', },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                style={{
                  flex: 1,
                  padding: '7px 0',
                  borderRadius: 8,
                  border: 'none',
                  cursor: 'pointer',
                  fontSize: 12,
                  fontWeight: 600,
                  fontFamily: "'DM Sans', sans-serif",
                  background: activeTab === tab.id ? t.tabActiveBg : 'transparent',
                  color: activeTab === tab.id ? t.tabActiveColor : t.tabColor,
                  transition: 'all .2s',
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* ─── CHAT TAB ──────────────────────────────── */}
          {activeTab === 'chat' && (
            <>
              {/* MESSAGES AREA */}
              <div
                className="mera-scrollbar"
                style={{
                  flex: 1,
                  overflowY: 'auto',
                  padding: '14px 14px 8px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 10,
                }}
              >
                {messages.map((msg, idx) => (
                  <div
                    key={idx}
                    className="mera-msg-enter"
                    style={{
                      alignSelf: msg.role === 'user' ? 'flex-end' : 'flex-start',
                      maxWidth: '85%',
                    }}
                  >
                    <div
                      style={{
                        padding: '10px 14px',
                        borderRadius:
                          msg.role === 'user'
                            ? '14px 14px 4px 14px'
                            : '14px 14px 14px 4px',
                        background:
                          msg.role === 'user'
                            ? 'linear-gradient(135deg, #0D9488, #0284C7)'
                            : t.msgBotBg,
                        border:
                          msg.role === 'user'
                            ? 'none'
                            : `1px solid ${t.msgBotBorder}`,
                        color: msg.role === 'user' ? '#fff' : t.msgBotColor,
                        fontSize: 13,
                        lineHeight: 1.65,
                      }}
                      dangerouslySetInnerHTML={{
                        __html: renderMarkdown(msg.content),
                      }}
                    />
                  </div>
                ))}

                {/* TYPING INDICATOR */}
                {isLoading && (
                  <div
                    className="mera-msg-enter"
                    style={{ alignSelf: 'flex-start', maxWidth: '85%' }}
                  >
                    <div
                      style={{
                        padding: '12px 18px',
                        borderRadius: '14px 14px 14px 4px',
                        background: t.msgBotBg,
                        border: `1px solid ${t.msgBotBorder}`,
                        display: 'flex',
                        gap: 5,
                        alignItems: 'center',
                      }}
                    >
                      {[0, 1, 2].map((i) => (
                        <span
                          key={i}
                          style={{
                            width: 7,
                            height: 7,
                            borderRadius: '50%',
                            background: '#0D9488',
                            display: 'inline-block',
                            animation: `meraTyping 1.4s ${i * 0.16}s infinite ease-in-out both`,
                          }}
                        />
                      ))}
                    </div>
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>

              {/* QUICK SUGGESTIONS */}
              {showSuggestions && messages.length <= 1 && (
                <div
                  style={{
                    padding: '0 14px 8px',
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 6,
                  }}
                >
                  {QUICK_SUGGESTIONS.map((s, i) => (
                    <button
                      key={i}
                      onClick={() => handleSend(s)}
                      style={{
                        padding: '6px 12px',
                        borderRadius: 20,
                        background: 'rgba(13,148,136,.1)',
                        border: '1px solid rgba(13,148,136,.25)',
                        color: '#0D9488',
                        fontSize: 11,
                        fontWeight: 500,
                        cursor: 'pointer',
                        fontFamily: "'DM Sans', sans-serif",
                        transition: 'all .2s',
                      }}
                      onMouseEnter={(e) => {
                        e.target.style.background = 'rgba(13,148,136,.2)';
                        e.target.style.borderColor = 'rgba(13,148,136,.5)';
                      }}
                      onMouseLeave={(e) => {
                        e.target.style.background = 'rgba(13,148,136,.1)';
                        e.target.style.borderColor = 'rgba(13,148,136,.25)';
                      }}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              )}

              {/* INPUT BAR */}
              <div
                style={{
                  padding: '10px 14px 14px',
                  borderTop: `1px solid ${t.inputBarBorder}`,
                  display: 'flex',
                  gap: 8,
                  alignItems: 'center',
                  background: t.inputBarBg,
                }}
              >
                <input
                  ref={inputRef}
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask Kira anything..."
                  disabled={isLoading}
                  style={{
                    flex: 1,
                    background: t.inputBg,
                    border: `1px solid ${t.inputBorder}`,
                    borderRadius: 12,
                    padding: '10px 14px',
                    color: t.inputColor,
                    fontSize: 13,
                    fontFamily: "'DM Sans', sans-serif",
                    outline: 'none',
                    transition: 'border-color .2s',
                  }}
                  onFocus={(e) => (e.target.style.borderColor = '#0D9488')}
                  onBlur={(e) => (e.target.style.borderColor = t.inputBorder)}
                />
                <button
                  onClick={() => handleSend()}
                  disabled={!input.trim() || isLoading}
                  aria-label="Send message"
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: 12,
                    background:
                      input.trim() && !isLoading
                        ? 'linear-gradient(135deg, #0D9488, #0284C7)'
                        : t.sendDisabledBg,
                    border: 'none',
                    color: input.trim() && !isLoading ? '#fff' : t.sendDisabledColor,
                    fontSize: 16,
                    cursor: input.trim() && !isLoading ? 'pointer' : 'not-allowed',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    transition: 'all .2s',
                    flexShrink: 0,
                  }}
                >
                  ➤
                </button>
              </div>
            </>
          )}

          {/* ─── FAQ TAB ───────────────────────────────── */}
          {activeTab === 'faq' && (
            <div
              className="mera-scrollbar"
              style={{
                flex: 1,
                overflowY: 'auto',
                padding: '10px 14px 14px',
                display: 'flex',
                flexDirection: 'column',
                gap: 6,
              }}
            >
              <div style={{ fontSize: 11, color: t.tabColor, marginBottom: 4, fontWeight: 500 }}>
                Frequently Asked Questions
              </div>
              {FAQ_ITEMS.map((faq, idx) => (
                <div
                  key={idx}
                  className="mera-faq-item"
                  onClick={() => setExpandedFaq(expandedFaq === idx ? null : idx)}
                  style={{
                    padding: '10px 14px',
                    borderRadius: 12,
                    background: expandedFaq === idx ? t.faqHoverBg : t.faqBg,
                    border: `1px solid ${t.faqBorder}`,
                    cursor: 'pointer',
                    transition: 'all .2s',
                  }}
                >
                  <div style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 8,
                  }}>
                    <span style={{
                      fontSize: 13,
                      fontWeight: 600,
                      color: t.faqQColor,
                      lineHeight: 1.4,
                    }}>
                      {faq.q}
                    </span>
                    <span style={{
                      fontSize: 14,
                      color: '#0D9488',
                      transform: expandedFaq === idx ? 'rotate(180deg)' : 'rotate(0)',
                      transition: 'transform .2s',
                      flexShrink: 0,
                    }}>
                      ▾
                    </span>
                  </div>
                  {expandedFaq === idx && (
                    <div style={{
                      marginTop: 8,
                      paddingTop: 8,
                      borderTop: `1px solid ${t.faqBorder}`,
                      fontSize: 12,
                      lineHeight: 1.7,
                      color: t.faqAColor,
                    }}>
                      {faq.a}
                    </div>
                  )}
                </div>
              ))}

              {/* Ask Kira link */}
              <div style={{ textAlign: 'center', marginTop: 8 }}>
                <button
                  onClick={() => setActiveTab('chat')}
                  style={{
                    padding: '8px 20px',
                    borderRadius: 20,
                    background: 'linear-gradient(135deg, #0D9488, #0284C7)',
                    border: 'none',
                    color: '#fff',
                    fontSize: 12,
                    fontWeight: 600,
                    cursor: 'pointer',
                    fontFamily: "'DM Sans', sans-serif",
                    transition: 'opacity .2s',
                  }}
                  onMouseEnter={(e) => (e.target.style.opacity = '0.85')}
                  onMouseLeave={(e) => (e.target.style.opacity = '1')}
                >
                  💬 Ask Kira for more help
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </>
  );
}
