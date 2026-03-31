<template>
  <div id="app" class="app-shell">
    <section class="hero">
      <div>
        <p class="eyebrow">Financial Management Dashboard</p>
        <h1>Assets, Transactions and Market Snapshot</h1>
        <p class="hero-copy">
          Asset now represents your current holdings. You can manually add buy and
          sell transactions below, and the backend will automatically update your holdings.
        </p>
      </div>
      <button class="refresh-button" :disabled="loading" @click="loadDashboard">
        {{ loading ? 'Loading...' : 'Refresh' }}
      </button>
    </section>

    <section v-if="error" class="error-banner">
      {{ error }}
    </section>

    <section class="summary-grid">
      <article class="summary-card accent-card">
        <span class="summary-label">assets</span>
        <strong>{{ assets.length }}</strong>
        <small>Current holdings tracked in the system</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">cost basis</span>
        <strong>{{ formatCurrency(totalCostBasis) }}</strong>
        <small>Total position cost across all holdings</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">market value</span>
        <strong>{{ formatCurrency(totalMarketValue) }}</strong>
        <small>Estimated by current price × quantity</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">p/l</span>
        <strong :class="pnlClass">{{ formatCurrency(totalPnL) }}</strong>
        <small>Unrealized income or loss on current holdings</small>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel quote-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">quote</p>
            <h2>Yahoo Finance / Fallback</h2>
          </div>
        </div>

        <form class="quote-form" @submit.prevent="searchQuote">
          <label>
            <span>Ticker</span>
            <input v-model.trim="tickerInput" type="text" placeholder="AAPL or 0700.HK">
          </label>
          <button type="submit" :disabled="quoteLoading">
            {{ quoteLoading ? 'Loading...' : 'Search' }}
          </button>
        </form>

        <div v-if="quoteError" class="inline-error">{{ quoteError }}</div>

        <div v-if="quote" class="quote-metrics">
          <div class="metric-box">
            <span>Price</span>
            <strong>{{ formatCurrency(quote.price) }}</strong>
          </div>
          <div class="metric-box">
            <span>Change</span>
            <strong :class="quoteTrendClass">{{ formatSigned(quote.change) }}</strong>
          </div>
          <div class="metric-box">
            <span>Source</span>
            <strong>{{ quote.source || 'unknown' }}</strong>
          </div>
          <div class="metric-box">
            <span>Time</span>
            <strong>{{ quote.latestTimestamp || 'realtime' }}</strong>
          </div>
        </div>

        <div ref="quoteChart" class="chart chart-medium"></div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">distribution</p>
            <h2>Asset Type Breakdown</h2>
          </div>
        </div>
        <div ref="typeChart" class="chart"></div>
      </article>
    </section>

    <section class="tables-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">assets</p>
            <h2>Current Holdings</h2>
          </div>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Name</th>
                <th>Type</th>
                <th>Quantity</th>
                <th>Avg Cost</th>
                <th>Current Price</th>
                <th>Cost Basis</th>
                <th>Current Value</th>
                <th>P/L</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="asset in assets" :key="asset.id">
                <td>{{ asset.ticker }}</td>
                <td>{{ asset.name }}</td>
                <td>{{ asset.type }}</td>
                <td>{{ formatNumber(asset.quantity) }}</td>
                <td>{{ formatCurrency(asset.avgCost) }}</td>
                <td>{{ formatCurrency(asset.currentPrice) }}</td>
                <td>{{ formatCurrency(asset.costBasis) }}</td>
                <td>{{ formatCurrency(asset.currentValue) }}</td>
                <td :class="valueClass(asset.unrealizedPnL)">
                  {{ formatCurrency(asset.unrealizedPnL) }}
                  <span class="rate-copy">({{ formatPercent(asset.unrealizedPnLRate) }})</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">transactions</p>
            <h2>Add Transaction</h2>
          </div>
        </div>

        <form class="transaction-form" @submit.prevent="submitTransaction">
          <label>
            <span>Type</span>
            <select v-model="transactionForm.transactionType">
              <option value="BUY">BUY</option>
              <option value="SELL">SELL</option>
            </select>
          </label>
          <label>
            <span>Ticker</span>
            <input v-model.trim="transactionForm.ticker" type="text" required placeholder="AAPL">
          </label>
          <label>
            <span>Name</span>
            <input v-model.trim="transactionForm.assetName" type="text" placeholder="Apple Inc.">
          </label>
          <label>
            <span>Type</span>
            <input v-model.trim="transactionForm.assetType" type="text" placeholder="STOCK">
          </label>
          <label>
            <span>Quantity</span>
            <input v-model.number="transactionForm.quantity" type="number" min="0.01" step="0.01" required>
          </label>
          <label>
            <span>Price</span>
            <input v-model.number="transactionForm.price" type="number" min="0.01" step="0.01" required>
          </label>
          <label>
            <span>Current Price</span>
            <input v-model.number="transactionForm.currentPrice" type="number" min="0.01" step="0.01" placeholder="Optional">
          </label>
          <label>
            <span>Date</span>
            <input v-model="transactionForm.transactionDate" type="datetime-local">
          </label>
          <label class="form-span-2">
            <span>Notes</span>
            <textarea v-model.trim="transactionForm.notes" rows="3" placeholder="Optional notes"></textarea>
          </label>

          <div class="form-actions form-span-2">
            <button type="button" class="ghost-button" @click="resetTransactionForm">Reset</button>
            <button type="submit" :disabled="submittingTransaction">
              {{ submittingTransaction ? 'Saving...' : 'Save Transaction' }}
            </button>
          </div>
        </form>
      </article>
    </section>

    <section class="panel">
      <div class="panel-heading">
        <div>
          <p class="panel-kicker">transactions</p>
          <h2>Transaction History</h2>
        </div>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th>Ticker</th>
              <th>Name</th>
              <th>Quantity</th>
              <th>Price</th>
              <th>Total</th>
              <th>Remaining</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="transaction in transactions" :key="transaction.id">
              <td>{{ formatDateTime(transaction.transactionDate) }}</td>
              <td :class="valueClass(transaction.transactionType === 'BUY' ? 1 : -1)">
                {{ transaction.transactionType }}
              </td>
              <td>{{ transaction.ticker }}</td>
              <td>{{ transaction.assetName || '--' }}</td>
              <td>{{ formatNumber(transaction.quantity) }}</td>
              <td>{{ formatCurrency(transaction.price) }}</td>
              <td>{{ formatCurrency(transaction.totalAmount) }}</td>
              <td>{{ formatNumber(transaction.remainingQuantity) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { createTransaction, fetchAssets, fetchQuote, fetchTransactions } from './services/api'

const createEmptyTransactionForm = () => ({
  transactionType: 'BUY',
  ticker: '',
  assetName: '',
  assetType: '',
  quantity: null,
  price: null,
  currentPrice: null,
  transactionDate: '',
  notes: ''
})

export default {
  name: 'App',
  data() {
    return {
      assets: [],
      transactions: [],
      quote: null,
      tickerInput: 'TSLA',
      loading: false,
      quoteLoading: false,
      submittingTransaction: false,
      error: '',
      quoteError: '',
      transactionForm: createEmptyTransactionForm(),
      charts: { type: null, quote: null }
    }
  },
  computed: {
    totalCostBasis() {
      return this.assets.reduce((sum, asset) => sum + Number(asset.costBasis || 0), 0)
    },
    totalMarketValue() {
      return this.assets.reduce((sum, asset) => sum + Number(asset.currentValue || 0), 0)
    },
    totalPnL() {
      return this.assets.reduce((sum, asset) => sum + Number(asset.unrealizedPnL || 0), 0)
    },
    pnlClass() {
      return this.valueClass(this.totalPnL)
    },
    quoteTrendClass() {
      return this.quote?.change >= 0 ? 'text-rise' : 'text-fall'
    }
  },
  mounted() {
    this.loadDashboard()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(chart => chart?.dispose())
  },
  methods: {
    async loadDashboard() {
      this.loading = true
      this.error = ''
      try {
        const [assets, transactions] = await Promise.all([
          fetchAssets(),
          fetchTransactions()
        ])
        this.assets = Array.isArray(assets) ? assets : []
        this.transactions = Array.isArray(transactions) ? transactions : []

        this.$nextTick(() => {
          this.renderTypeChart()
        })

        if (!this.quote) {
          await this.searchQuote()
        }
      } catch (error) {
        this.error = `Load failed: ${error.message || 'Unknown error'}`
      } finally {
        this.loading = false
      }
    },
    async searchQuote() {
      if (!this.tickerInput) {
        return
      }
      this.quoteLoading = true
      this.quoteError = ''
      try {
        this.quote = await fetchQuote(this.tickerInput)
        this.$nextTick(() => this.renderQuoteChart())
      } catch (error) {
        this.quoteError = `Quote failed: ${error.message || 'Unknown error'}`
      } finally {
        this.quoteLoading = false
      }
    },
    async submitTransaction() {
      this.submittingTransaction = true
      this.error = ''
      try {
        const payload = {
          ...this.transactionForm,
          ticker: this.transactionForm.ticker?.trim(),
          assetName: this.transactionForm.assetName?.trim() || null,
          assetType: this.transactionForm.assetType?.trim() || null,
          transactionDate: this.transactionForm.transactionDate
            ? new Date(this.transactionForm.transactionDate).toISOString()
            : null,
          notes: this.transactionForm.notes?.trim() || null
        }
        await createTransaction(payload)
        this.resetTransactionForm()
        await this.loadDashboard()
      } catch (error) {
        this.error = `Transaction failed: ${error.message || 'Unknown error'}`
      } finally {
        this.submittingTransaction = false
      }
    },
    resetTransactionForm() {
      this.transactionForm = createEmptyTransactionForm()
    },
    formatCurrency(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'USD',
        maximumFractionDigits: 2
      }).format(Number(value))
    },
    formatNumber(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
    },
    formatPercent(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      return `${(Number(value) * 100).toFixed(2)}%`
    },
    formatSigned(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      const numeric = Number(value)
      return `${numeric >= 0 ? '+' : ''}${numeric.toFixed(2)}`
    },
    formatDateTime(value) {
      if (!value) {
        return '--'
      }
      return new Date(value).toLocaleString('zh-CN')
    },
    valueClass(value) {
      return Number(value || 0) >= 0 ? 'text-rise' : 'text-fall'
    },
    getChartInst(key, el) {
      if (!this.charts[key] && el) {
        this.charts[key] = echarts.init(el)
      }
      return this.charts[key]
    },
    renderTypeChart() {
      const chart = this.getChartInst('type', this.$refs.typeChart)
      if (!chart) {
        return
      }

      const grouped = {}
      this.assets.forEach(asset => {
        const assetType = asset.type || 'UNKNOWN'
        grouped[assetType] = (grouped[assetType] || 0) + Number(asset.currentValue || 0)
      })

      chart.setOption({
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            data: Object.entries(grouped).map(([name, value]) => ({
              name,
              value
            }))
          }
        ]
      })
    },
    renderQuoteChart() {
      const chart = this.getChartInst('quote', this.$refs.quoteChart)
      if (!chart || !this.quote) {
        return
      }

      chart.setOption({
        xAxis: { data: ['Open', 'Prev Close', 'Low', 'High', 'Price'] },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            smooth: true,
            data: [
              this.quote.open,
              this.quote.previousClose,
              this.quote.dayLow,
              this.quote.dayHigh,
              this.quote.price
            ]
          }
        ]
      })
    },
    handleResize() {
      Object.values(this.charts).forEach(chart => chart?.resize())
    }
  }
}
</script>

<style>
:root {
  --bg: #06101d;
  --panel: rgba(10, 23, 42, 0.88);
  --line: rgba(126, 156, 199, 0.2);
  --text: #eef4ff;
  --muted: #9db1d0;
  --accent: #39d0a4;
  --danger: #ff7a7a;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background: #07111f;
  color: var(--text);
  font-family: sans-serif;
}

button,
input,
select,
textarea {
  font: inherit;
}

.app-shell {
  min-height: 100vh;
  padding: 32px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
  padding: 28px;
  border-radius: 28px;
  background: rgba(12, 31, 57, 0.95);
}

.eyebrow,
.summary-label,
.panel-kicker {
  color: var(--accent);
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero h1 {
  font-size: 42px;
  margin: 10px 0;
}

.hero-copy {
  color: var(--muted);
  line-height: 1.7;
  max-width: 680px;
}

.refresh-button,
.quote-form button,
.transaction-form button {
  padding: 14px 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, #39d0a4, #2b7fff);
  border: none;
  font-weight: bold;
  cursor: pointer;
}

.ghost-button {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.16);
  color: var(--text);
}

.error-banner,
.inline-error {
  background: rgba(255, 122, 122, 0.2);
  padding: 14px;
  border-radius: 18px;
  margin: 20px 0;
  color: #ffd4d4;
}

.summary-grid,
.content-grid,
.tables-grid {
  display: grid;
  gap: 20px;
  margin: 20px 0;
}

.summary-grid {
  grid-template-columns: repeat(4, 1fr);
}

.content-grid {
  grid-template-columns: 1.2fr 1fr;
}

.tables-grid {
  grid-template-columns: 1.5fr 1fr;
}

.summary-card,
.panel {
  padding: 22px;
  background: var(--panel);
  border-radius: 24px;
  border: 1px solid var(--line);
}

.accent-card {
  background: rgba(57, 208, 164, 0.1);
}

.summary-card strong {
  font-size: 34px;
  display: block;
  margin: 8px 0;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart {
  width: 100%;
  height: 340px;
}

.chart-medium {
  height: 280px;
}

.quote-form {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
}

.quote-form label {
  width: 100%;
}

.quote-form span,
.transaction-form span {
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
}

.quote-form input,
.transaction-form input,
.transaction-form select,
.transaction-form textarea {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 14px;
  border-radius: 14px;
  color: #fff;
}

.quote-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.metric-box {
  padding: 14px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 18px;
}

.transaction-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-span-2 {
  grid-column: span 2;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 14px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
  vertical-align: top;
}

.rate-copy {
  color: var(--muted);
  margin-left: 6px;
  font-size: 12px;
}

.text-rise {
  color: #39d0a4;
}

.text-fall {
  color: #ff7a7a;
}

@media (max-width: 1200px) {
  .summary-grid,
  .content-grid,
  .tables-grid,
  .quote-metrics,
  .transaction-form {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .app-shell {
    padding: 16px;
  }

  .hero,
  .summary-grid,
  .content-grid,
  .tables-grid,
  .quote-metrics,
  .transaction-form {
    grid-template-columns: 1fr;
  }

  .hero {
    display: block;
  }

  .quote-form,
  .form-actions {
    flex-direction: column;
  }

  .form-span-2 {
    grid-column: span 1;
  }
}
</style>
