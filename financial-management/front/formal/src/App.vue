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
      <div class="hero-actions">
        <button class="ghost-button hero-button" :disabled="resettingSampleData || loading" @click="handleResetSampleData">
          {{ resettingSampleData ? 'Resetting...' : 'Reset Sample Data' }}
        </button>
        <button class="refresh-button hero-button" :disabled="loading" @click="loadDashboard">
          {{ loading ? 'Loading...' : 'Refresh' }}
        </button>
      </div>
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

    <section class="performance-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">performance</p>
            <h2>Total Holdings P/L Trend</h2>
          </div>
        </div>
        <div ref="portfolioPnlChart" class="chart chart-medium"></div>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel quote-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">quote</p>
            <h2>Finnhub / Historical Fallback</h2>
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

    <section class="analytics-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">portfolio</p>
            <h2>Portfolio Value History</h2>
          </div>
        </div>
        <div ref="portfolioHistoryChart" class="chart chart-medium"></div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">holdings</p>
            <h2>Holdings Value History</h2>
          </div>
        </div>
        <div ref="holdingsHistoryChart" class="chart chart-medium"></div>
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
                <th>Trend</th>
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
                <td class="trend-cell">
                  <svg
                    v-if="getHoldingTrendSeries(asset.ticker).length"
                    class="sparkline"
                    viewBox="0 0 120 36"
                    preserveAspectRatio="none"
                  >
                    <path
                      :d="sparklinePath(getHoldingTrendSeries(asset.ticker), 120, 36)"
                      :class="['sparkline-path', valueClass(getLatestHoldingPnl(asset.ticker))]"
                    />
                  </svg>
                  <span v-else class="trend-empty">--</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <section class="panel full-width-panel">
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
          <input
            v-model.trim="transactionForm.ticker"
            type="text"
            required
            placeholder="AAPL"
            @input="applyTickerDefaults"
          >
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
          <div class="picker-row">
            <input
              ref="transactionDateInput"
              v-model="transactionForm.transactionDate"
              type="datetime-local"
              step="60"
              @focus="openDatePicker"
              @click="openDatePicker"
            >
            <button type="button" class="picker-button" @click="openDatePicker">Pick</button>
          </div>
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
import {
  createTransaction,
  fetchAssets,
  fetchHoldingsHistory,
  fetchPortfolioHistory,
  fetchQuote,
  fetchTransactions,
  resetSampleData
} from './services/api'

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

const TICKER_PRESETS = {
  AAPL: { assetName: 'Apple Inc.', assetType: 'STOCK' },
  TSLA: { assetName: 'Tesla Inc.', assetType: 'STOCK' },
  MSFT: { assetName: 'Microsoft Corp.', assetType: 'STOCK' },
  AMZN: { assetName: 'Amazon.com Inc.', assetType: 'STOCK' },
  META: { assetName: 'Meta Platforms Inc.', assetType: 'STOCK' },
  NVDA: { assetName: 'NVIDIA Corp.', assetType: 'STOCK' },
  C: { assetName: 'Citigroup Inc.', assetType: 'STOCK' }
}

export default {
  name: 'App',
  data() {
    return {
      assets: [],
      transactions: [],
      holdingsHistory: [],
      portfolioHistory: [],
      quote: null,
      tickerInput: 'TSLA',
      loading: false,
      quoteLoading: false,
      submittingTransaction: false,
      resettingSampleData: false,
      transactionQuoteLoading: false,
      error: '',
      quoteError: '',
      transactionForm: createEmptyTransactionForm(),
      charts: { type: null, quote: null, portfolioPnl: null, portfolioHistory: null, holdingsHistory: null },
      transactionLookupToken: 0
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
        const [assetsResult, transactionsResult, holdingsHistoryResult, portfolioHistoryResult] = await Promise.allSettled([
          fetchAssets(),
          fetchTransactions(),
          fetchHoldingsHistory(),
          fetchPortfolioHistory()
        ])

        if (assetsResult.status === 'rejected') {
          throw assetsResult.reason
        }
        if (transactionsResult.status === 'rejected') {
          throw transactionsResult.reason
        }

        this.assets = Array.isArray(assetsResult.value) ? assetsResult.value : []
        this.transactions = Array.isArray(transactionsResult.value) ? transactionsResult.value : []
        this.holdingsHistory = holdingsHistoryResult.status === 'fulfilled' && Array.isArray(holdingsHistoryResult.value)
          ? holdingsHistoryResult.value
          : []
        this.portfolioHistory = portfolioHistoryResult.status === 'fulfilled' && Array.isArray(portfolioHistoryResult.value)
          ? portfolioHistoryResult.value
          : []

        this.$nextTick(() => {
          this.renderTypeChart()
          this.renderPortfolioPnlChart()
          this.renderPortfolioHistoryChart()
          this.renderHoldingsHistoryChart()
        })

        if (holdingsHistoryResult.status === 'rejected' || portfolioHistoryResult.status === 'rejected') {
          this.error = 'Historical charts are temporarily unavailable, but holdings and transactions loaded successfully.'
        }

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
    async handleResetSampleData() {
      this.resettingSampleData = true
      this.error = ''
      try {
        await resetSampleData()
        this.quote = null
        await this.loadDashboard()
      } catch (error) {
        this.error = `Reset failed: ${error.message || 'Unknown error'}`
      } finally {
        this.resettingSampleData = false
      }
    },
    applyTickerDefaults() {
      const ticker = this.transactionForm.ticker?.trim()?.toUpperCase()
      if (!ticker) {
        return
      }
      this.transactionForm.ticker = ticker
      const preset = TICKER_PRESETS[ticker]
      if (!preset) {
        return
      }
      if (!this.transactionForm.assetName) {
        this.transactionForm.assetName = preset.assetName
      }
      if (!this.transactionForm.assetType) {
        this.transactionForm.assetType = preset.assetType
      }
      this.lookupTransactionCurrentPrice(ticker)
    },
    async lookupTransactionCurrentPrice(ticker) {
      const normalizedTicker = ticker?.trim()?.toUpperCase()
      if (!normalizedTicker) {
        return
      }

      const lookupToken = ++this.transactionLookupToken
      this.transactionQuoteLoading = true
      try {
        const quote = await fetchQuote(normalizedTicker)
        if (lookupToken !== this.transactionLookupToken || this.transactionForm.ticker !== normalizedTicker) {
          return
        }
        if (quote?.price != null) {
          this.transactionForm.currentPrice = Number(quote.price)
        }
        if (!this.transactionForm.assetName && quote?.name) {
          this.transactionForm.assetName = quote.name
        }
      } catch (error) {
        // Keep the form usable even if quote lookup fails.
      } finally {
        if (lookupToken === this.transactionLookupToken) {
          this.transactionQuoteLoading = false
        }
      }
    },
    openDatePicker() {
      const input = this.$refs.transactionDateInput
      if (input && typeof input.showPicker === 'function') {
        input.showPicker()
      }
    },
    resetTransactionForm() {
      this.transactionLookupToken += 1
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
    formatCompactCurrency(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD',
        notation: 'compact',
        maximumFractionDigits: 1
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
    renderPortfolioHistoryChart() {
      const chart = this.getChartInst('portfolioHistory', this.$refs.portfolioHistoryChart)
      if (!chart) {
        return
      }

      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: this.portfolioHistory.map(point => point.timestamp)
        },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            smooth: true,
            areaStyle: {},
            data: this.portfolioHistory.map(point => Number(point.value || 0))
          }
        ]
      })
    },
    renderPortfolioPnlChart() {
      const chart = this.getChartInst('portfolioPnl', this.$refs.portfolioPnlChart)
      if (!chart) {
        return
      }

      const pnlSeries = this.portfolioHistory.map(point => Number(point.pnl || 0))
      const latestPnl = pnlSeries.length ? pnlSeries[pnlSeries.length - 1] : 0
      const trendColor = latestPnl >= 0 ? '#39d0a4' : '#ff7a7a'

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => this.formatCurrency(value)
        },
        grid: {
          left: 56,
          right: 20,
          top: 24,
          bottom: 32
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.portfolioHistory.map(point => point.timestamp)
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: value => this.formatCompactCurrency(value)
          }
        },
        series: [
          {
            type: 'line',
            smooth: true,
            showSymbol: false,
            lineStyle: { width: 3, color: trendColor },
            itemStyle: { color: trendColor },
            areaStyle: {
              color: latestPnl >= 0 ? 'rgba(57, 208, 164, 0.12)' : 'rgba(255, 122, 122, 0.12)'
            },
            markLine: {
              symbol: 'none',
              lineStyle: {
                color: 'rgba(255, 255, 255, 0.18)',
                type: 'dashed'
              },
              data: [{ yAxis: 0 }]
            },
            data: pnlSeries
          }
        ]
      })
    },
    renderHoldingsHistoryChart() {
      const chart = this.getChartInst('holdingsHistory', this.$refs.holdingsHistoryChart)
      if (!chart) {
        return
      }

      const categories = this.holdingsHistory[0]?.points?.map(point => point.timestamp) || []
      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: {
          top: 0,
          textStyle: { color: '#eef4ff' }
        },
        xAxis: {
          type: 'category',
          data: categories
        },
        yAxis: { type: 'value' },
        series: this.holdingsHistory.map(history => ({
          name: history.ticker,
          type: 'line',
          smooth: true,
          data: (history.points || []).map(point => Number(point.value || 0))
        }))
      })
    },
    getHoldingTrendSeries(ticker) {
      const history = this.holdingsHistory.find(item => item.ticker === ticker)
      return (history?.points || []).map(point => Number(point.pnl || 0))
    },
    getLatestHoldingPnl(ticker) {
      const series = this.getHoldingTrendSeries(ticker)
      return series.length ? series[series.length - 1] : 0
    },
    sparklinePath(values, width = 120, height = 36) {
      if (!Array.isArray(values) || !values.length) {
        return ''
      }
      if (values.length === 1) {
        const centerY = height / 2
        return `M 0 ${centerY} L ${width} ${centerY}`
      }

      const numbers = values.map(value => Number(value || 0))
      const min = Math.min(...numbers)
      const max = Math.max(...numbers)
      const range = max - min || 1

      return numbers.map((value, index) => {
        const x = (index / (numbers.length - 1)) * width
        const y = height - ((value - min) / range) * height
        return `${index === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`
      }).join(' ')
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

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hero-button {
  white-space: nowrap;
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
.performance-grid,
.content-grid,
.analytics-grid,
.tables-grid {
  display: grid;
  gap: 20px;
  margin: 20px 0;
}

.summary-grid {
  grid-template-columns: repeat(4, 1fr);
}

.performance-grid {
  grid-template-columns: 1fr;
}

.content-grid {
  grid-template-columns: 1.2fr 1fr;
}

.analytics-grid {
  grid-template-columns: 1fr 1fr;
}

.tables-grid {
  grid-template-columns: 1fr;
}

.summary-card,
.panel {
  padding: 22px;
  background: var(--panel);
  border-radius: 24px;
  border: 1px solid var(--line);
}

.full-width-panel {
  margin: 20px 0;
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

.picker-row {
  display: flex;
  gap: 10px;
}

.picker-button {
  flex: 0 0 auto;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: var(--text);
  cursor: pointer;
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

.trend-cell {
  min-width: 132px;
}

.sparkline {
  display: block;
  width: 120px;
  height: 36px;
}

.sparkline-path {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sparkline-path.text-rise {
  stroke: var(--accent);
}

.sparkline-path.text-fall {
  stroke: var(--danger);
}

.trend-empty {
  color: var(--muted);
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
  .performance-grid,
  .content-grid,
  .analytics-grid,
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
  .performance-grid,
  .content-grid,
  .analytics-grid,
  .tables-grid,
  .quote-metrics,
  .transaction-form {
    grid-template-columns: 1fr;
  }

  .hero {
    display: block;
  }

  .quote-form,
  .form-actions,
  .hero-actions {
    flex-direction: column;
  }

  .form-span-2 {
    grid-column: span 1;
  }
}
</style>
