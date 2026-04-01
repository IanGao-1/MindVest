<template>
  <div id="app" class="app-shell">
    <section class="hero">
      <div class="hero-brand">
        <div class="brand-badge">
          <img src="/huifeng.png" alt="Huifeng logo" class="brand-logo">
          <div>
            <p class="eyebrow">Huifeng Capital</p>
            <h1>Assets, Transactions and Market Snapshot</h1>
          </div>
        </div>
        <p class="eyebrow">Financial Management Dashboard</p>
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
        <form class="history-form" @submit.prevent="handleHistorySearch">
          <label>
            <span>Ticker</span>
            <input
              v-model.trim="historyTickerInput"
              type="text"
              placeholder="AAPL"
            >
          </label>
          <label>
            <span>Range</span>
            <select v-model="historyRange">
              <option value="1M">1 Month</option>
              <option value="6M">6 Months</option>
            </select>
          </label>
          <button type="submit" :disabled="historyLoading">
            {{ historyLoading ? 'Loading...' : 'Search' }}
          </button>
        </form>
        <div v-if="historyError" class="inline-error">{{ historyError }}</div>
        <div v-else-if="selectedHistoryMeta" class="history-summary">
          <div class="metric-box">
            <span>Ticker</span>
            <strong>{{ selectedHistoryMeta.symbol }}</strong>
          </div>
          <div class="metric-box">
            <span>Range</span>
            <strong>{{ historyRangeLabel }}</strong>
          </div>
          <div class="metric-box">
            <span>Latest Close</span>
            <strong>{{ formatCurrency(selectedHistoryMeta.latestClose) }}</strong>
          </div>
          <div class="metric-box">
            <span>Last Refreshed</span>
            <strong>{{ selectedHistoryMeta.lastRefreshed }}</strong>
          </div>
        </div>
        <div ref="portfolioHistoryChart" class="chart chart-medium"></div>
      </article>

<!--      <article class="panel">-->
<!--        <div class="panel-heading">-->
<!--          <div>-->
<!--            <p class="panel-kicker">holdings</p>-->
<!--            <h2>Holdings Value History</h2>-->
<!--          </div>-->
<!--        </div>-->
<!--        <div ref="holdingsHistoryChart" class="chart chart-medium"></div>-->
<!--      </article>-->
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
                <td :class="holdingValueClass(asset.unrealizedPnL)">
                  {{ formatCurrency(asset.unrealizedPnL) }}
                  <span class="rate-copy">({{ formatPercent(asset.unrealizedPnLRate) }})</span>
                </td>
                <td class="trend-cell">
                  <div
                    v-if="getHoldingTrendSeries(asset.ticker, '1M').length"
                    class="trend-hover-card"
                    @mouseenter="showHoldingTrendPopover(asset, $event)"
                    @mouseleave="hideHoldingTrendPopover"
                  >
                    <svg
                      class="sparkline"
                      viewBox="0 0 96 28"
                      preserveAspectRatio="none"
                    >
                      <path
                        :d="sparklinePath(getHoldingTrendSeries(asset.ticker, '1M'), 96, 28)"
                        :class="['sparkline-path', holdingValueClass(getHoldingTrendDelta(asset.ticker, '1M'))]"
                      />
                    </svg>
                  </div>
                  <span v-else class="trend-empty">--</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </section>

    <div
      v-if="hoveredTrend"
      class="trend-floating-popover"
      :style="hoveredTrend.style"
    >
      <div class="trend-popover-header">
        <strong>{{ hoveredTrend.ticker }}</strong>
        <span>1 Month Close</span>
      </div>
      <svg
        class="sparkline sparkline-large"
        viewBox="0 0 180 72"
        preserveAspectRatio="none"
      >
        <path
          :d="sparklinePath(hoveredTrend.series, 180, 72)"
          :class="['sparkline-path', holdingValueClass(hoveredTrend.delta)]"
        />
      </svg>
      <div class="trend-popover-meta">
        <span>{{ formatCurrency(hoveredTrend.firstClose) }}</span>
        <span :class="holdingValueClass(hoveredTrend.delta)">
          {{ formatSigned(hoveredTrend.delta) }}
        </span>
        <span>{{ formatCurrency(hoveredTrend.lastClose) }}</span>
      </div>
    </div>

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
  fetchHistoryJson,
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
  AMZN: { assetName: 'Amazon.com Inc.', assetType: 'STOCK' },
  BND: { assetName: 'Vanguard Total Bond Market ETF', assetType: 'ETF' },
  C: { assetName: 'Citigroup Inc.', assetType: 'STOCK' },
  GLD: { assetName: 'SPDR Gold Shares', assetType: 'ETF' },
  META: { assetName: 'Meta Platforms Inc.', assetType: 'STOCK' },
  MSFT: { assetName: 'Microsoft Corp.', assetType: 'STOCK' },
  NVDA: { assetName: 'NVIDIA Corp.', assetType: 'STOCK' },
  QQQ: { assetName: 'Invesco QQQ Trust', assetType: 'ETF' },
  SLV: { assetName: 'iShares Silver Trust', assetType: 'ETF' },
  SPY: { assetName: 'SPDR S&P 500 ETF Trust', assetType: 'ETF' },
  TLT: { assetName: 'iShares 20+ Year Treasury Bond ETF', assetType: 'ETF' },
  TSLA: { assetName: 'Tesla Inc.', assetType: 'STOCK' },
  VNQ: { assetName: 'Vanguard Real Estate Index Fund', assetType: 'ETF' },
  VOO: { assetName: 'Vanguard S&P 500 ETF', assetType: 'ETF' }
}

const HISTORY_TICKER_ALIASES = {
  APPL: 'AAPL'
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
      historyTickerInput: 'AAPL',
      historyRange: '1M',
      historyLoading: false,
      historyError: '',
      selectedHistory: null,
      selectedHistoryMeta: null,
      holdingTrendHistories: {},
      hoveredTrend: null,
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
    },
    historyRangeLabel() {
      return this.historyRange === '6M' ? '6 Months' : '1 Month'
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

        if (holdingsHistoryResult.status === 'rejected' || portfolioHistoryResult.status === 'rejected') {
          this.error = 'Historical charts are temporarily unavailable, but holdings and transactions loaded successfully.'
        }

        if (!this.quote) {
          await this.searchQuote()
        }
        await this.loadHoldingTrendHistories()
        this.$nextTick(() => {
          this.renderTypeChart()
          this.renderPortfolioPnlChart()
          this.renderHoldingsHistoryChart()
        })
        await this.handleHistorySearch()
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
      const normalizedTicker = this.normalizeHistoryTicker(ticker)
      if (!normalizedTicker) {
        return
      }

      const lookupToken = ++this.transactionLookupToken
      this.transactionQuoteLoading = true
      try {
        let quote = null
        try {
          quote = await fetchQuote(normalizedTicker)
        } catch (error) {
          quote = null
        }

        if (
          lookupToken !== this.transactionLookupToken ||
          this.normalizeHistoryTicker(this.transactionForm.ticker) !== normalizedTicker
        ) {
          return
        }

        if (quote?.price != null) {
          this.transactionForm.currentPrice = Number(quote.price)
        } else {
          const history = await fetchHistoryJson(normalizedTicker)
          const latestClose = this.getLatestCloseFromHistory(history)
          if (
            lookupToken !== this.transactionLookupToken ||
            this.normalizeHistoryTicker(this.transactionForm.ticker) !== normalizedTicker
          ) {
            return
          }
          if (latestClose != null) {
            this.transactionForm.currentPrice = latestClose
          }
        }

        if (!this.transactionForm.assetName && quote?.name) {
          this.transactionForm.assetName = quote.name
        }
      } catch (error) {
        try {
          const history = await fetchHistoryJson(normalizedTicker)
          const latestClose = this.getLatestCloseFromHistory(history)
          if (
            lookupToken !== this.transactionLookupToken ||
            this.normalizeHistoryTicker(this.transactionForm.ticker) !== normalizedTicker
          ) {
            return
          }
          if (latestClose != null) {
            this.transactionForm.currentPrice = latestClose
          }
        } catch (historyError) {
          // Keep the form usable even if both quote and history lookup fail.
        }
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
      const numericValue = Number(value)
      const compactValue = new Intl.NumberFormat('en-US', {
        notation: 'compact',
        maximumFractionDigits: 1
      }).format(numericValue)
      return numericValue < 0 ? `-$${compactValue.slice(1)}` : `$${compactValue}`
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
    holdingValueClass(value) {
      return Number(value || 0) >= 0 ? 'text-holding-rise' : 'text-holding-fall'
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

      const quoteSeries = [
        this.quote.open,
        this.quote.previousClose,
        this.quote.dayLow,
        this.quote.dayHigh,
        this.quote.price
      ].map(value => (value == null || Number.isNaN(Number(value)) ? null : Number(value)))
      const quoteRange = this.buildChartRange(quoteSeries)

      chart.setOption({
        xAxis: { data: ['Open', 'Prev Close', 'Low', 'High', 'Price'] },
        yAxis: {
          type: 'value',
          min: quoteRange.min,
          max: quoteRange.max
        },
        series: [
          {
            type: 'line',
            smooth: true,
            showSymbol: true,
            symbolSize: 8,
            lineStyle: {
              width: 3,
              color: '#db3f16'
            },
            itemStyle: {
              color: '#db3f16'
            },
            areaStyle: {
              color: 'rgba(219, 63, 22, 0.12)'
            },
            data: quoteSeries
          }
        ]
      })
    },
    renderPortfolioHistoryChart() {
      const chart = this.getChartInst('portfolioHistory', this.$refs.portfolioHistoryChart)
      if (!chart) {
        return
      }

      const historyPoints = this.getSelectedHistoryPoints()
      if (!historyPoints.length) {
        chart.clear()
        return
      }
      const closeSeries = historyPoints.map(point => point.close)
      const historyRange = this.buildChartRange(closeSeries)

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: params => {
            const point = Array.isArray(params) ? params[0] : params
            if (!point) {
              return ''
            }

            const numericValue = Number(point.value || 0)
            const valueColor = numericValue < 0 ? '#2e8b57' : '#db3f16'

            return `
              <div style="min-width: 126px;">
                <div style="margin-bottom: 8px; color: #7c6c5d;">${point.axisValue}</div>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <span style="width: 10px; height: 10px; border-radius: 50%; background: ${point.color || valueColor}; display: inline-block;"></span>
                  <span style="color: ${valueColor}; font-weight: 700;">${this.formatCurrency(numericValue)}</span>
                </div>
              </div>
            `
          }
        },
        grid: {
          left: 56,
          right: 24,
          top: 24,
          bottom: 40
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: historyPoints.map(point => point.date)
        },
        yAxis: {
          type: 'value',
          min: historyRange.min,
          max: historyRange.max,
          axisLabel: {
            formatter: value => this.formatCompactCurrency(value)
          }
        },
        series: [
          {
            type: 'line',
            smooth: true,
            showSymbol: false,
            lineStyle: {
              width: 3,
              color: '#db3f16'
            },
            itemStyle: {
              color: '#db3f16'
            },
            areaStyle: {
              color: 'rgba(219, 63, 22, 0.12)'
            },
            data: historyPoints.map(point => point.close)
          }
        ]
      })
    },
    async handleHistorySearch() {
      this.historyLoading = true
      this.historyError = ''

      try {
        const normalizedTicker = this.normalizeHistoryTicker(this.historyTickerInput)
        const history = await fetchHistoryJson(normalizedTicker)

        if (!history) {
          throw new Error(`No history response found for ${normalizedTicker}`)
        }

        this.historyTickerInput = normalizedTicker
        this.selectedHistory = history
        const filteredPrices = this.getFilteredHistoryPrices(history.prices)

        if (!filteredPrices.length) {
          throw new Error(`No ${this.historyRangeLabel} data available for ${normalizedTicker}`)
        }

        const latestPoint = filteredPrices[filteredPrices.length - 1]
        this.selectedHistoryMeta = {
          symbol: normalizedTicker,
          lastRefreshed: history.lastRefreshed || latestPoint.date,
          latestClose: Number(latestPoint.close || 0)
        }

        this.$nextTick(() => this.renderPortfolioHistoryChart())
      } catch (error) {
        this.selectedHistory = null
        this.selectedHistoryMeta = null
        this.historyError = `History failed: ${error.message || 'Unknown error'}`
        this.$nextTick(() => {
          const chart = this.getChartInst('portfolioHistory', this.$refs.portfolioHistoryChart)
          chart?.clear()
        })
      } finally {
        this.historyLoading = false
      }
    },
    async loadHoldingTrendHistories() {
      const uniqueTickers = [...new Set(
        this.assets
          .map(asset => this.normalizeHistoryTicker(asset.ticker))
          .filter(Boolean)
      )]

      if (!uniqueTickers.length) {
        this.holdingTrendHistories = {}
        return
      }

      const results = await Promise.allSettled(
        uniqueTickers.map(async ticker => [ticker, await fetchHistoryJson(ticker)])
      )

      this.holdingTrendHistories = results.reduce((historyMap, result) => {
        if (result.status === 'fulfilled') {
          const [ticker, history] = result.value
          historyMap[ticker] = history
        }
        return historyMap
      }, {})
    },
    normalizeHistoryTicker(ticker) {
      const normalizedTicker = String(ticker || '').trim().toUpperCase()
      return HISTORY_TICKER_ALIASES[normalizedTicker] || normalizedTicker
    },
    getSelectedHistoryPoints() {
      return this.getFilteredHistoryPrices(this.selectedHistory?.prices || [])
    },
    getFilteredHistoryPrices(prices, range = this.historyRange) {
      if (!Array.isArray(prices) || !prices.length) {
        return []
      }

      const sortedPrices = [...prices]
        .filter(point => point?.date && point?.close != null)
        .sort((first, second) => new Date(first.date) - new Date(second.date))

      if (!sortedPrices.length) {
        return []
      }

      const latestDate = new Date(sortedPrices[sortedPrices.length - 1].date)
      const startDate = new Date(latestDate)
      startDate.setMonth(startDate.getMonth() - (range === '6M' ? 6 : 1))

      return sortedPrices.map(point => ({
        date: point.date,
        close: Number(point.close || 0)
      })).filter(point => new Date(point.date) >= startDate)
    },
    getLatestCloseFromHistory(history) {
      const prices = Array.isArray(history?.prices) ? history.prices : []
      if (!prices.length) {
        return null
      }

      const latestPoint = [...prices]
        .filter(point => point?.date && point?.close != null)
        .sort((first, second) => new Date(second.date) - new Date(first.date))[0]

      return latestPoint ? Number(latestPoint.close || 0) : null
    },
    buildChartRange(values, padding = 10) {
      const numericValues = (values || []).map(value => Number(value)).filter(value => !Number.isNaN(value))
      if (!numericValues.length) {
        return { min: 0, max: 100 }
      }

      const minValue = Math.min(...numericValues)
      const maxValue = Math.max(...numericValues)
      if (minValue === maxValue) {
        return {
          min: minValue - padding,
          max: maxValue + padding
        }
      }

      return {
        min: minValue - padding,
        max: maxValue + padding
      }
    },
    getPortfolioPnlTrendPoints(range = '1M') {
      const normalizedAssets = this.assets
        .map(asset => ({
          ticker: this.normalizeHistoryTicker(asset.ticker),
          quantity: Number(asset.quantity || 0),
          avgCost: Number(asset.avgCost || 0)
        }))
        .filter(asset => asset.ticker && asset.quantity > 0)

      if (!normalizedAssets.length) {
        return []
      }

      const dateSet = new Set()
      const assetSeriesMap = {}

      normalizedAssets.forEach(asset => {
        const history = this.holdingTrendHistories[asset.ticker]
        const series = this.getFilteredHistoryPrices(history?.prices || [], range)
        if (!series.length) {
          return
        }

        assetSeriesMap[asset.ticker] = series
        series.forEach(point => dateSet.add(point.date))
      })

      const sortedDates = [...dateSet].sort((first, second) => new Date(first) - new Date(second))
      if (!sortedDates.length) {
        return []
      }

      return sortedDates.map(date => {
        let totalPnl = 0

        normalizedAssets.forEach(asset => {
          const series = assetSeriesMap[asset.ticker] || []
          const closingPoint = this.getLatestHistoryPointByDate(series, date)
          if (!closingPoint) {
            return
          }

          totalPnl += (Number(closingPoint.close || 0) - asset.avgCost) * asset.quantity
        })

        return {
          date,
          pnl: totalPnl
        }
      })
    },
    getLatestHistoryPointByDate(series, targetDate) {
      if (!Array.isArray(series) || !series.length) {
        return null
      }

      const targetTimestamp = new Date(targetDate).getTime()
      let matchedPoint = null

      series.forEach(point => {
        const pointTimestamp = new Date(point.date).getTime()
        if (pointTimestamp <= targetTimestamp) {
          matchedPoint = point
        }
      })

      return matchedPoint
    },
    renderPortfolioPnlChart() {
      const chart = this.getChartInst('portfolioPnl', this.$refs.portfolioPnlChart)
      if (!chart) {
        return
      }

      const pnlPoints = this.getPortfolioPnlTrendPoints('1M')
      if (!pnlPoints.length) {
        chart.clear()
        return
      }

      const pnlSeries = pnlPoints.map(point => Number(point.pnl || 0))
      const pnlRange = this.buildChartRange(pnlSeries)
      const latestPnl = pnlSeries.length ? pnlSeries[pnlSeries.length - 1] : 0
      const trendColor = latestPnl >= 0 ? '#db3f16' : '#8f3a2d'

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
          data: pnlPoints.map(point => point.date)
        },
        yAxis: {
          type: 'value',
          min: pnlRange.min,
          max: pnlRange.max,
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
              color: latestPnl >= 0 ? 'rgba(219, 63, 22, 0.12)' : 'rgba(143, 58, 45, 0.12)'
            },
            markLine: {
              symbol: 'none',
              lineStyle: {
                color: 'rgba(34, 27, 21, 0.22)',
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
          textStyle: { color: '#221b15' }
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
    getHoldingTrendSeries(ticker, range = this.historyRange) {
      const normalizedTicker = this.normalizeHistoryTicker(ticker)
      const history = this.holdingTrendHistories[normalizedTicker]
      return this.getFilteredHistoryPrices(history?.prices || [], range).map(point => Number(point.close || 0))
    },
    getHoldingTrendDelta(ticker, range = this.historyRange) {
      const series = this.getHoldingTrendSeries(ticker, range)
      if (series.length < 2) {
        return 0
      }
      return series[series.length - 1] - series[0]
    },
    getHoldingTrendFirstClose(ticker, range = this.historyRange) {
      const series = this.getHoldingTrendSeries(ticker, range)
      return series.length ? series[0] : null
    },
    getHoldingTrendLastClose(ticker, range = this.historyRange) {
      const series = this.getHoldingTrendSeries(ticker, range)
      return series.length ? series[series.length - 1] : null
    },
    showHoldingTrendPopover(asset, event) {
      const series = this.getHoldingTrendSeries(asset.ticker, '1M')
      if (!series.length) {
        this.hoveredTrend = null
        return
      }

      const triggerRect = event.currentTarget.getBoundingClientRect()
      const popoverWidth = 220
      const popoverHeight = 142
      const viewportPadding = 12
      const centeredLeft = triggerRect.left + (triggerRect.width / 2) - (popoverWidth / 2)
      const left = Math.min(
        window.innerWidth - popoverWidth - viewportPadding,
        Math.max(viewportPadding, centeredLeft)
      )
      const top = triggerRect.top >= popoverHeight + 16
        ? triggerRect.top - popoverHeight - 10
        : Math.min(window.innerHeight - popoverHeight - viewportPadding, triggerRect.bottom + 10)

      this.hoveredTrend = {
        ticker: this.normalizeHistoryTicker(asset.ticker),
        series,
        delta: this.getHoldingTrendDelta(asset.ticker, '1M'),
        firstClose: this.getHoldingTrendFirstClose(asset.ticker, '1M'),
        lastClose: this.getHoldingTrendLastClose(asset.ticker, '1M'),
        style: {
          left: `${left}px`,
          top: `${top}px`
        }
      }
    },
    hideHoldingTrendPopover() {
      this.hoveredTrend = null
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
      this.hideHoldingTrendPopover()
      Object.values(this.charts).forEach(chart => chart?.resize())
    }
  }
}
</script>

<style>
:root {
  --bg: #f6f1ea;
  --panel: rgba(255, 255, 255, 0.92);
  --line: rgba(34, 27, 21, 0.1);
  --text: #221b15;
  --muted: #7c6c5d;
  --accent: #db3f16;
  --danger: #8f3a2d;
  --surface: #fffdf9;
  --surface-strong: #fffaf4;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background:
    radial-gradient(circle at top right, rgba(219, 63, 22, 0.12), transparent 28%),
    linear-gradient(180deg, #faf6f0 0%, #f4efe8 100%);
  color: var(--text);
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

button,
input,
select,
textarea {
  font: inherit;
}

.app-shell {
  min-height: 100vh;
  max-width: 1480px;
  margin: 0 auto;
  padding: 28px 32px 40px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  padding: 30px 32px;
  border-radius: 30px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 246, 240, 0.96));
  border: 1px solid rgba(219, 63, 22, 0.12);
  box-shadow: 0 22px 50px rgba(55, 37, 23, 0.08);
}

.hero-brand {
  flex: 1;
}

.brand-badge {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 10px;
}

.brand-logo {
  width: 72px;
  height: 72px;
  object-fit: contain;
  border-radius: 20px;
  background: #ffffff;
  padding: 10px;
  box-shadow: 0 12px 28px rgba(34, 27, 21, 0.12);
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-self: center;
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
.history-form button,
.transaction-form button {
  padding: 14px 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, #db3f16, #f05a28);
  border: none;
  color: #fffaf5;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 14px 24px rgba(219, 63, 22, 0.18);
}

.ghost-button {
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(34, 27, 21, 0.12);
  color: var(--text);
}

.error-banner,
.inline-error {
  background: rgba(219, 63, 22, 0.08);
  padding: 14px;
  border-radius: 18px;
  margin: 20px 0;
  color: #9d341d;
  border: 1px solid rgba(219, 63, 22, 0.12);
}

.summary-grid,
.performance-grid,
.content-grid,
.analytics-grid,
.tables-grid {
  display: grid;
  gap: 20px;
  margin: 24px 0;
}

.summary-grid {
  grid-template-columns: repeat(4, 1fr);
}

.performance-grid {
  grid-template-columns: 1fr;
}

.content-grid {
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
}

.analytics-grid {
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
}

.tables-grid {
  grid-template-columns: 1fr;
}

.summary-card,
.panel {
  padding: 24px;
  background: var(--panel);
  border-radius: 24px;
  border: 1px solid var(--line);
  box-shadow: 0 14px 34px rgba(55, 37, 23, 0.05);
}

.full-width-panel {
  margin: 24px 0;
}

.accent-card {
  background: linear-gradient(135deg, rgba(219, 63, 22, 0.14), rgba(255, 248, 241, 0.96));
  border-color: rgba(219, 63, 22, 0.16);
}

.summary-card strong {
  font-size: 34px;
  display: block;
  margin: 8px 0;
}

.summary-card small {
  line-height: 1.6;
}

.panel-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
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

.history-form {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
  align-items: end;
}

.quote-form label {
  width: 100%;
}

.history-form label {
  flex: 1;
}

.quote-form span,
.transaction-form span {
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
}

.history-form span {
  display: block;
  margin-bottom: 8px;
  color: var(--muted);
}

.quote-form input,
.history-form input,
.history-form select,
.transaction-form input,
.transaction-form select,
.transaction-form textarea {
  width: 100%;
  background: var(--surface);
  border: 1px solid rgba(34, 27, 21, 0.12);
  padding: 14px;
  border-radius: 14px;
  color: var(--text);
}

.picker-row {
  display: flex;
  gap: 10px;
}

.picker-button {
  flex: 0 0 auto;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(34, 27, 21, 0.12);
  background: var(--surface);
  color: var(--text);
  cursor: pointer;
}

.quote-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.history-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.metric-box {
  padding: 14px;
  background: var(--surface-strong);
  border-radius: 18px;
  border: 1px solid rgba(34, 27, 21, 0.06);
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
  min-width: 1120px;
}

th,
td {
  padding: 14px;
  text-align: left;
  border-bottom: 1px solid rgba(34, 27, 21, 0.08);
  vertical-align: top;
}

.trend-cell {
  min-width: 108px;
}

.sparkline {
  display: block;
  width: 96px;
  height: 28px;
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

.sparkline-path.text-holding-rise {
  stroke: #db3f16;
}

.sparkline-path.text-holding-fall {
  stroke: #2e8b57;
}

.trend-empty {
  color: var(--muted);
}

.trend-hover-card {
  display: inline-flex;
  align-items: center;
}

.trend-hover-card .sparkline {
  transition: transform 0.18s ease, filter 0.18s ease;
}

.trend-hover-card:hover .sparkline {
  transform: scale(1.08);
  filter: drop-shadow(0 8px 18px rgba(219, 63, 22, 0.18));
}

.trend-floating-popover {
  position: fixed;
  width: 220px;
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 252, 247, 0.98);
  border: 1px solid rgba(219, 63, 22, 0.14);
  box-shadow: 0 18px 42px rgba(34, 27, 21, 0.14);
  pointer-events: none;
  z-index: 9999;
}

.transaction-form {
  align-items: start;
}

.trend-popover-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}

.trend-popover-header strong {
  font-size: 14px;
}

.trend-popover-header span,
.trend-popover-meta span {
  color: var(--muted);
  font-size: 12px;
}

.sparkline-large {
  width: 100%;
  height: 72px;
}

.trend-popover-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.rate-copy {
  color: var(--muted);
  margin-left: 6px;
  font-size: 12px;
}

.text-rise {
  color: var(--accent);
}

.text-fall {
  color: var(--danger);
}

.text-holding-rise {
  color: #db3f16;
}

.text-holding-fall {
  color: #2e8b57;
}

@media (max-width: 1200px) {
  .summary-grid,
  .performance-grid,
  .content-grid,
  .analytics-grid,
  .tables-grid,
  .quote-metrics,
  .history-summary,
  .transaction-form {
    grid-template-columns: repeat(2, 1fr);
  }

  table {
    min-width: 980px;
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
  .history-summary,
  .transaction-form {
    grid-template-columns: 1fr;
  }

  .hero {
    display: block;
  }

  .brand-badge {
    align-items: flex-start;
  }

  .quote-form,
  .history-form,
  .form-actions,
  .hero-actions {
    flex-direction: column;
  }

  .form-span-2 {
    grid-column: span 1;
  }
}
</style>
