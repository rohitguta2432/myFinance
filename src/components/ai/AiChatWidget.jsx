import { useState, useRef, useEffect } from 'react';

// ─── QUICK SUGGESTION CHIPS ─────────────────────────
const QUICK_SUGGESTIONS = [
  "What's my savings rate?",
  "How can I save on taxes?",
  "How much emergency fund do I need?",
  "Am I on track for retirement?",
  "Is my insurance cover enough?",
];

// ─── API CALL ────────────────────────────────────────
async function sendMessage(message, history, financialContext) {
  const response = await fetch('/api/v1/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, history, financialContext }),
  });
  if (!response.ok) throw new Error(`Chat API error: ${response.status}`);
  return response.json();
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

// ─── CHAT WIDGET COMPONENT ──────────────────────────
export default function AiChatWidget({ user }) {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Hello! 👋 I\'m **Mera** — your personal financial advisor.\n\nAsk me anything about your finances. I\'ll give you personalized advice based on your financial profile.\n\nWhat would you like to know?',
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
    if (isOpen) setTimeout(() => inputRef.current?.focus(), 300);
  }, [isOpen]);

  const handleSend = async (text) => {
    const msg = text || input.trim();
    if (!msg || isLoading) return;

    setInput('');
    setShowSuggestions(false);

    // Add user message
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
            'Sorry, I\'m experiencing a technical issue right now. Please try again in a moment. 🙏',
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
      .replace(/`(.*?)`/g, '<code style="background:#1E293B;padding:1px 5px;border-radius:4px;font-size:11px">$1</code>')
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
      `}</style>

      {/* ─── FLOATING BUTTON ─────────────────────────── */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          aria-label="Open Mera AI Assistant"
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
            background: 'rgba(15, 23, 42, 0.97)',
            border: '1px solid rgba(51, 65, 85, 0.6)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            boxShadow: '0 25px 60px rgba(0,0,0,.6), 0 0 40px rgba(13,148,136,.15)',
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
              background: 'linear-gradient(135deg, rgba(13,148,136,.2), rgba(2,132,199,.15))',
              borderBottom: '1px solid rgba(51,65,85,.5)',
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
                  color: '#F1F5F9',
                  fontFamily: 'Georgia, serif',
                }}
              >
                Mera
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

          {/* ─── MESSAGES AREA ─────────────────────────── */}
          <div
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
                        : 'rgba(30, 41, 59, 0.8)',
                    border:
                      msg.role === 'user'
                        ? 'none'
                        : '1px solid rgba(51,65,85,.5)',
                    color: msg.role === 'user' ? '#fff' : '#CBD5E1',
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
                    background: 'rgba(30, 41, 59, 0.8)',
                    border: '1px solid rgba(51,65,85,.5)',
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

          {/* ─── QUICK SUGGESTIONS ─────────────────────── */}
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

          {/* ─── INPUT BAR ─────────────────────────────── */}
          <div
            style={{
              padding: '10px 14px 14px',
              borderTop: '1px solid rgba(51,65,85,.4)',
              display: 'flex',
              gap: 8,
              alignItems: 'center',
              background: 'rgba(7,11,20,.5)',
            }}
          >
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask Mera anything..."
              disabled={isLoading}
              style={{
                flex: 1,
                background: '#1E293B',
                border: '1px solid #334155',
                borderRadius: 12,
                padding: '10px 14px',
                color: '#F1F5F9',
                fontSize: 13,
                fontFamily: "'DM Sans', sans-serif",
                outline: 'none',
                transition: 'border-color .2s',
              }}
              onFocus={(e) => (e.target.style.borderColor = '#0D9488')}
              onBlur={(e) => (e.target.style.borderColor = '#334155')}
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
                    : '#1E293B',
                border: 'none',
                color: input.trim() && !isLoading ? '#fff' : '#475569',
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
        </div>
      )}
    </>
  );
}
