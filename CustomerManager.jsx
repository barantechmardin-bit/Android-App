import React, { useState, useMemo } from 'react';

// Tailwind is assumed for visual styling as per platform design guidelines
export default function CustomerManager() {
  // 1. Initial State with multi-scenario realistic data
  const [customers, setCustomers] = useState([
    {
      id: 1,
      name: "Mehmet Emre Baran",
      phone: "+90 532 123 4567",
      email: "mehmetemrebaran@gmail.com",
      balance: -1500.00 // Negative indicates debt
    },
    {
      id: 2,
      name: "Ahmet Yılmaz (Yılmaz Ticaret)",
      phone: "+90 542 987 6543",
      email: "info@yilmazticaret.com",
      balance: 500.00 // Positive indicates prepayment/deposit credit
    },
    {
      id: 3,
      name: "Ayberk Demir",
      phone: "+90 555 456 7890",
      email: "ayberk@demirholding.com",
      balance: 0.00 // Neutral
    }
  ]);

  // 2. Individual form inputs state
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [initialBalance, setInitialBalance] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [showAddForm, setShowAddForm] = useState(false);

  // 3. Balance transaction state for individual customer editing
  const [selectedCustomerId, setSelectedCustomerId] = useState(null);
  const [transactionAmount, setTransactionAmount] = useState('');
  const [transactionType, setTransactionType] = useState('charge'); // 'charge' (add debt/increase invoice) or 'payment' (decrease debt/pay)

  // 4. Input validation and Customer addition handler
  const handleAddCustomer = (e) => {
    e.preventDefault();
    if (!name.trim() || !phone.trim() || !email.trim()) {
      alert("Name, phone and email fields are required!");
      return;
    }

    const newCustomer = {
      id: Date.now(),
      name: name.trim(),
      phone: phone.trim(),
      email: email.trim(),
      balance: parseFloat(initialBalance) || 0.0
    };

    setCustomers(prev => [newCustomer, ...prev]);
    
    // Clear Form fields
    setName('');
    setPhone('');
    setEmail('');
    setInitialBalance('');
    setShowAddForm(false);
  };

  // 5. Delete handler
  const handleDeleteCustomer = (id) => {
    if (window.confirm("Are you sure you want to delete this customer record?")) {
      setCustomers(prev => prev.filter(c => c.id !== id));
      if (selectedCustomerId === id) setSelectedCustomerId(null);
    }
  };

  // 6. Balance update transaction handler (local state adjustment)
  const handleBalanceTransaction = (e) => {
    e.preventDefault();
    const amount = parseFloat(transactionAmount);
    if (isNaN(amount) || amount <= 0) {
      alert("Please enter a valid amount greater than zero!");
      return;
    }

    setCustomers(prev => prev.map(c => {
      if (c.id === selectedCustomerId) {
        // 'charge' increases positive balance / decreases cash owe depending on nomenclature.
        // Here, we define: 'charge' adds to balance (client gets credit), 'payment' subtracts. 
        // Or positive balance is prepayed credit, negative is customer owe.
        // Let's standardise: charging increases customer's balance value, payment reduces it (client pays off).
        const change = transactionType === 'deposit' ? amount : -amount;
        return { ...c, balance: Number((c.balance + change).toFixed(2)) };
      }
      return c;
    }));

    setTransactionAmount('');
    setSelectedCustomerId(null);
  };

  // 7. Memoized filtered list for real-time search
  const filteredCustomers = useMemo(() => {
    return customers.filter(c => 
      c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.phone.includes(searchQuery) ||
      c.email.toLowerCase().includes(searchQuery.toLowerCase())
    );
  }, [customers, searchQuery]);

  // Combined totals selector
  const totals = useMemo(() => {
    return customers.reduce((acc, curr) => {
      if (curr.balance < 0) {
        acc.totalReceivables += Math.abs(curr.balance);
      } else {
        acc.totalPrepayments += curr.balance;
      }
      return acc;
    }, { totalReceivables: 0, totalPrepayments: 0 });
  }, [customers]);

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 p-6 font-sans">
      <div className="max-w-6xl mx-auto space-y-6">
        
        {/* Header Branding Panel */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between border-b border-slate-800 pb-5">
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
              👤 Customer Management System
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Store customer records, emails, phone numbers, and calculate total balances dynamically.
            </p>
          </div>
          <button
            onClick={() => setShowAddForm(!showAddForm)}
            className={`mt-4 md:mt-0 px-4 py-2.5 rounded-lg font-semibold text-sm transition-all duration-200 shadow-md flex items-center gap-2 ${
              showAddForm 
                ? 'bg-rose-600 hover:bg-rose-500 text-white' 
                : 'bg-emerald-600 hover:bg-emerald-500 text-white'
            }`}
          >
            {showAddForm ? '❌ Cancel Form' : '➕ Register New Customer'}
          </button>
        </div>

        {/* Dynamic Aggregated Metrics */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-slate-800 border border-slate-700/60 p-5 rounded-xl shadow-lg">
            <span className="text-xs text-slate-400 font-bold uppercase tracking-wider">Total Customer Records</span>
            <div className="text-3xl font-extrabold text-white mt-1">{customers.length}</div>
            <p className="text-xs text-slate-500 mt-1">Active local database entries</p>
          </div>
          <div className="bg-slate-800 border border-slate-700/60 p-5 rounded-xl shadow-lg">
            <span className="text-xs text-emerald-400 font-bold uppercase tracking-wider">Total Customer Prepayments</span>
            <div className="text-3xl font-extrabold text-emerald-400 mt-1">
               +{totals.totalPrepayments.toLocaleString('tr-TR', { minimumFractionDigits: 2 })} TL
            </div>
            <p className="text-xs text-slate-500 mt-1">Funds deposited by customers</p>
          </div>
          <div className="bg-slate-800 border border-slate-700/60 p-5 rounded-xl shadow-lg">
            <span className="text-xs text-rose-400 font-bold uppercase tracking-wider">Total Active Receivables ({`Owed`})</span>
            <div className="text-3xl font-extrabold text-rose-400 mt-1">
              -{totals.totalReceivables.toLocaleString('tr-TR', { minimumFractionDigits: 2 })} TL
            </div>
            <p className="text-xs text-slate-500 mt-1">Pending payments from customers</p>
          </div>
        </div>

        {/* Add Customer Form Modal-like panel */}
        {showAddForm && (
          <form onSubmit={handleAddCustomer} className="bg-slate-800 border border-emerald-500/30 p-6 rounded-xl space-y-4 animate-fadeIn">
            <h3 className="text-lg font-bold text-emerald-400">Add New Customer Record</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-1">Full Name / Company Name *</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Alperen Çelik"
                  value={name}
                  onChange={e => setName(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 focus:border-emerald-500 rounded-lg p-2.5 text-white outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-1">Phone Number *</label>
                <input
                  type="tel"
                  required
                  placeholder="e.g. +90 532 111 2233"
                  value={phone}
                  onChange={e => setPhone(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 focus:border-emerald-500 rounded-lg p-2.5 text-white outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-1">Email Address *</label>
                <input
                  type="email"
                  required
                  placeholder="e.g. alperen@gmail.com"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 focus:border-emerald-500 rounded-lg p-2.5 text-white outline-none"
                />
              </div>
              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-1">Initial Balance (TL)</label>
                <input
                  type="number"
                  step="0.01"
                  placeholder="Leave 0 if neutral, use negative for initial debt"
                  value={initialBalance}
                  onChange={e => setInitialBalance(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 focus:border-emerald-500 rounded-lg p-2.5 text-white outline-none"
                />
              </div>
            </div>
            <div className="pt-2 flex justify-end">
              <button
                type="submit"
                className="bg-emerald-600 hover:bg-emerald-500 text-white font-bold py-2.5 px-6 rounded-lg transition-all text-sm"
              >
                Create Customer Card
              </button>
            </div>
          </form>
        )}

        {/* Transactional Balance Editing Quick Modal */}
        {selectedCustomerId !== null && (
          <form onSubmit={handleBalanceTransaction} className="bg-slate-800 border border-amber-500/30 p-6 rounded-xl space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="text-lg font-bold text-amber-400">
                Adjust Balance for: <span className="text-white">{customers.find(c => c.id === selectedCustomerId)?.name}</span>
              </h3>
              <button type="button" onClick={() => setSelectedCustomerId(null)} className="text-slate-400 hover:text-white">✕</button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-end">
              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-1">Adjustment Type</label>
                <select
                  value={transactionType}
                  onChange={e => setTransactionType(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-white outline-none focus:border-amber-500"
                >
                  <option value="deposit">Deposit Prepayment (+ Increase)</option>
                  <option value="charge">Charge Debt / Debit (- Decrease)</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-bold uppercase text-slate-400 mb-1">Amount (TL) *</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="Enter amount"
                  value={transactionAmount}
                  onChange={e => setTransactionAmount(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg p-2.5 text-white outline-none focus:border-amber-500"
                />
              </div>
              <button
                type="submit"
                className="bg-amber-600 hover:bg-amber-500 text-slate-950 font-extrabold py-2.5 px-4 rounded-lg transition-all text-sm"
              >
                Apply Local Balance Change
              </button>
            </div>
          </form>
        )}

        {/* Real-time search Filter and Customers list */}
        <div className="bg-slate-800 rounded-xl border border-slate-700/50 p-6 space-y-4">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              📋 Filtered Customer List ({filteredCustomers.length})
            </h2>
            <div className="relative w-full md:w-80">
              <input
                type="text"
                placeholder="Search by name, phone, email..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 focus:border-emerald-500 rounded-lg px-4 py-2 text-sm text-white outline-none pl-10"
              />
              <span className="absolute left-3 top-2.5 text-slate-500">🔍</span>
            </div>
          </div>

          {filteredCustomers.length === 0 ? (
            <div className="text-center py-12 text-slate-500 border border-dashed border-slate-700 rounded-xl">
              <p className="text-lg">No customer records match your query.</p>
              <p className="text-sm text-slate-600 mt-1">Try refining your search text or add a new record.</p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-lg">
              <table className="w-full text-left text-sm text-slate-300">
                <thead className="bg-slate-900 text-slate-400 uppercase text-xs font-bold">
                  <tr>
                    <th className="px-5 py-3.5">Full Name</th>
                    <th className="px-5 py-3.5">Phone</th>
                    <th className="px-5 py-3.5">Email</th>
                    <th className="px-5 py-3.5 text-right">Current Balance (TL)</th>
                    <th className="px-5 py-3.5 text-center">Form Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-700/50">
                  {filteredCustomers.map(customer => {
                    const absBalance = Math.abs(customer.balance).toLocaleString('tr-TR', { minimumFractionDigits: 2 });
                    return (
                      <tr key={customer.id} className="hover:bg-slate-700/30 transition-colors">
                        <td className="px-5 py-4 font-semibold text-white">{customer.name}</td>
                        <td className="px-5 py-4 font-mono">{customer.phone}</td>
                        <td className="px-5 py-4">{customer.email}</td>
                        <td className="px-5 py-4 text-right">
                          <span className={`inline-block font-extrabold px-3 py-1 rounded-full text-xs ${
                            customer.balance > 0 
                              ? 'bg-emerald-400/10 text-emerald-400 border border-emerald-400/20' 
                              : customer.balance < 0 
                                ? 'bg-rose-400/10 text-rose-400 border border-rose-400/20' 
                                : 'bg-slate-400/10 text-slate-400 border border-slate-400/20'
                          }`}>
                            {customer.balance > 0 ? `+${absBalance}` : customer.balance < 0 ? `-${absBalance}` : '0,00'} TL
                          </span>
                        </td>
                        <td className="px-5 py-4 text-center">
                          <div className="flex items-center justify-center gap-2">
                            <button
                              onClick={() => {
                                setSelectedCustomerId(customer.id);
                                setTransactionType(customer.balance >= 0 ? 'charge' : 'deposit');
                              }}
                              className="bg-slate-900 hover:bg-slate-700 text-amber-400 hover:text-amber-300 font-bold text-xs py-1.5 px-3 rounded border border-slate-700"
                            >
                              💸 Adjust Balance
                            </button>
                            <button
                              onClick={() => handleDeleteCustomer(customer.id)}
                              className="bg-rose-950 hover:bg-rose-900 text-rose-400 hover:text-rose-300 font-bold text-xs py-1.5 px-3 rounded border border-rose-900/40"
                            >
                              ✕ Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

      </div>
    </div>
  );
}
