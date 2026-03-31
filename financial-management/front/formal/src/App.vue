<template>
  <div id="app" class="app-shell">
    <section class="hero">
      <div>
        <p class="eyebrow">Financial Management Dashboard</p>
        <h1>Assets, Portfolios and Market Snapshot</h1>
        <p class="hero-copy">
          The dashboard connects to the Spring Boot backend and displays assets,
          portfolios, cash balance, and live quote data with ECharts.
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
      <article class="summary-card">
        <span class="summary-label">balance</span>
        <strong>{{ formatCurrency(balance) }}</strong>
        <small>Account cash balance</small>
      </article>
      <article class="summary-card accent-card">
        <span class="summary-label">assets</span>
        <strong>{{ assets.length }}</strong>
        <small>Total assets currently tracked</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">portfolios</span>
        <strong>{{ portfolios.length }}</strong>
        <small>Portfolio count</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">market value</span>
        <strong>{{ formatCurrency(totalMarketValue) }}</strong>
        <small>Estimated by current price × quantity</small>
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

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">portfolio</p>
            <h2>Portfolio Value Ranking</h2>
          </div>
        </div>
        <div ref="portfolioChart" class="chart"></div>
      </article>
    </section>

    <section class="tables-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">assets</p>
            <h2>Assets</h2>
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
                <th>Market Value</th>
                <th>Action</th>
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
                <td>{{ formatCurrency(assetValue(asset)) }}</td>
                <td class="action-cell">
                  <button class="action-button buy-btn" @click="openBuyModal(asset)">Buy</button>
                  <button class="sell-btn" @click="openSellModal(asset)">Sell</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">portfolios</p>
            <h2>Portfolios</h2>
          </div>
        </div>
        <div class="portfolio-list">
          <div v-for="portfolio in portfolios" :key="portfolio.id" class="portfolio-item">
            <div class="portfolio-row">
              <strong>{{ portfolio.name }}</strong>
              <span>{{ formatCurrency(portfolio.totalValue) }}</span>
            </div>
            <p>{{ portfolio.description || 'No description' }}</p>
            <div class="portfolio-meta">
              <span>{{ portfolio.assetCount || 0 }} assets</span>
              <span>{{ formatDate(portfolio.createdAt) }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>

    <div v-if="showBuyModal" class="modal" @click.self="closeBuyModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>Buy Asset</h3>
          <button class="close-btn" @click="closeBuyModal">x</button>
        </div>
        <form class="asset-form" @submit.prevent="submitBuy">
          <div class="form-group">
            <label>Ticker</label>
            <input :value="buyForm.ticker" readonly>
          </div>
          <div class="form-group">
            <label>Name</label>
            <input :value="buyForm.name" readonly>
          </div>
          <div class="form-group">
            <label>Type</label>
            <input :value="buyForm.type" readonly>
          </div>
          <div class="form-group">
            <label>Quantity</label>
            <input v-model.number="buyForm.quantity" type="number" min="0.01" step="0.01" required>
          </div>
          <div class="form-group">
            <label>Current Price</label>
            <input :value="formatCurrency(buyForm.currentPrice)" readonly>
          </div>
          <div class="modal-footer">
            <button type="button" @click="closeBuyModal">Cancel</button>
            <button type="submit" :disabled="submitting">
              {{ submitting ? 'Submitting...' : 'Confirm Buy' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-if="showSellModal" class="modal" @click.self="closeSellModal">
      <div class="modal-content small-modal">
        <div class="modal-header">
          <h3>Confirm Sell</h3>
          <button class="close-btn" @click="closeSellModal">x</button>
        </div>
        <form class="asset-form" @submit.prevent="submitSell">
          <div class="form-group">
            <label>Ticker</label>
            <input :value="currentAsset?.ticker || ''" readonly>
          </div>
          <div class="form-group">
            <label>Holding Quantity</label>
            <input :value="formatNumber(currentAsset?.quantity)" readonly>
          </div>
          <div class="form-group">
            <label>Sell Price</label>
            <input :value="formatCurrency(currentAsset?.currentPrice)" readonly>
          </div>
          <div class="form-group">
            <label>Sell Quantity</label>
            <input
              v-model.number="sellQuantity"
              type="number"
              min="0.01"
              step="0.01"
              :max="currentAsset?.quantity || undefined"
              required
            >
          </div>
          <div class="modal-footer">
            <button type="button" @click="closeSellModal">Cancel</button>
            <button class="sell-submit" type="submit" :disabled="submitting">
              {{ submitting ? 'Selling...' : 'Confirm Sell' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { fetchAssets, fetchBalance, fetchPortfolios, fetchQuote } from './services/api'

const createAsset = async (data) => {
  const res = await fetch('/api/assets', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data)
  })
  if (!res.ok) throw new Error('Buy failed')
  return res.json()
}

const sellAsset = async (id, quantity) => {
  const res = await fetch(`/api/assets/${id}/sell`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ quantity })
  })
  if (!res.ok) throw new Error('Sell failed')
  if (res.status === 204) return null
  return res.json()
}

const createEmptyBuyForm = () => ({
  ticker: '',
  name: '',
  type: '',
  quantity: 0,
  avgCost: 0,
  currentPrice: 0
})

export default {
  name: 'App',
  data() {
    return {
      assets: [],
      balance: 0,
      portfolios: [],
      quote: null,
      tickerInput: 'TSLA',
      loading: false,
      quoteLoading: false,
      error: '',
      quoteError: '',
      charts: { type: null, portfolio: null, quote: null },
      showBuyModal: false,
      showSellModal: false,
      currentAsset: null,
      sellQuantity: 0,
      submitting: false,
      buyForm: createEmptyBuyForm()
    }
  },
  computed: {
    totalMarketValue() {
      return this.assets.reduce((sum, asset) => sum + this.assetValue(asset), 0)
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
    Object.values(this.charts).forEach(c => c?.dispose())
  },
  methods: {
    async loadDashboard() {
      this.loading = true
      this.error = ''
      try {
        await Promise.all([this.refreshAssets(), this.refreshPortfolios()])
        this.$nextTick(() => {
          this.renderTypeChart()
          this.renderPortfolioChart()
        })
        if (!this.quote) {
          this.searchQuote()
        }
      } catch (e) {
        this.error = `Load failed: ${e.message}`
      } finally {
        this.loading = false
      }
    },
    async searchQuote() {
      if (!this.tickerInput) return
      this.quoteLoading = true
      this.quoteError = ''
      try {
        this.quote = await fetchQuote(this.tickerInput)
        this.$nextTick(() => this.renderQuoteChart())
      } catch (e) {
        this.quoteError = `Quote failed: ${e.message}`
      } finally {
        this.quoteLoading = false
      }
    },
    assetValue(asset) {
      return Number(asset.currentPrice || 0) * Number(asset.quantity || 0)
    },
    async refreshAssets() {
      const [assets, balanceResponse] = await Promise.all([
        fetchAssets(),
        fetchBalance()
      ])
      this.assets = Array.isArray(assets) ? assets : []
      this.balance = Number(balanceResponse?.balance || 0)
    },
    async refreshPortfolios() {
      const portfolios = await fetchPortfolios()
      this.portfolios = Array.isArray(portfolios) ? portfolios : []
    },
    formatCurrency(v) {
      return v == null ? '--' : new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: 'USD'
      }).format(v)
    },
    formatNumber(v) {
      return v == null ? '--' : Number(v).toFixed(2)
    },
    formatSigned(v) {
      if (v == null || Number.isNaN(Number(v))) return '--'
      return Number(v) >= 0 ? `+${Number(v).toFixed(2)}` : Number(v).toFixed(2)
    },
    formatDate(v) {
      return v ? new Date(v).toLocaleDateString() : '--'
    },
    getChartInst(key, el) {
      if (!this.charts[key] && el) this.charts[key] = echarts.init(el)
      return this.charts[key]
    },
    renderTypeChart() {
      const c = this.getChartInst('type', this.$refs.typeChart)
      if (!c) return
      const group = {}
      this.assets.forEach((a) => {
        group[a.type] = (group[a.type] || 0) + this.assetValue(a)
      })
      c.setOption({
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            data: Object.entries(group).map(([k, v]) => ({ name: k, value: v }))
          }
        ]
      })
    },
    renderPortfolioChart() {
      const c = this.getChartInst('portfolio', this.$refs.portfolioChart)
      if (!c) return
      const list = [...this.portfolios]
        .sort((a, b) => Number(b.totalValue || 0) - Number(a.totalValue || 0))
        .slice(0, 8)
      c.setOption({
        xAxis: { type: 'category', data: list.map((i) => i.name) },
        yAxis: { type: 'value' },
        series: [{ type: 'bar', data: list.map((i) => i.totalValue) }]
      })
    },
    renderQuoteChart() {
      const c = this.getChartInst('quote', this.$refs.quoteChart)
      if (!c || !this.quote) return
      const data = [
        this.quote.open,
        this.quote.previousClose,
        this.quote.dayLow,
        this.quote.dayHigh,
        this.quote.price
      ]
      c.setOption({
        xAxis: { data: ['Open', 'Prev Close', 'Low', 'High', 'Price'] },
        yAxis: { type: 'value' },
        series: [{ type: 'line', smooth: true, data }]
      })
    },
    handleResize() {
      Object.values(this.charts).forEach(c => c?.resize())
    },
    openBuyModal(asset) {
      const dealPrice = Number(asset?.currentPrice ?? asset?.avgCost ?? 0)
      this.showBuyModal = true
      this.buyForm = {
        ticker: asset?.ticker || '',
        name: asset?.name || '',
        type: asset?.type || '',
        quantity: 0,
        avgCost: dealPrice,
        currentPrice: dealPrice
      }
    },
    closeBuyModal() {
      this.showBuyModal = false
      this.buyForm = createEmptyBuyForm()
    },
    async submitBuy() {
      this.submitting = true
      this.error = ''
      try {
        await createAsset(this.buyForm)
        await this.refreshAssets()
        await this.refreshPortfolios()
        this.$nextTick(() => {
          this.renderTypeChart()
          this.renderPortfolioChart()
        })
        this.closeBuyModal()
      } catch (e) {
        this.error = e.message
      } finally {
        this.submitting = false
      }
    },
    openSellModal(asset) {
      this.showSellModal = true
      this.currentAsset = asset
      this.sellQuantity = 0
    },
    closeSellModal() {
      this.showSellModal = false
      this.currentAsset = null
      this.sellQuantity = 0
    },
    async submitSell() {
      this.submitting = true
      this.error = ''
      try {
        await sellAsset(this.currentAsset.id, this.sellQuantity)
        await this.refreshAssets()
        await this.refreshPortfolios()
        this.$nextTick(() => {
          this.renderTypeChart()
          this.renderPortfolioChart()
        })
        this.closeSellModal()
      } catch (e) {
        this.error = e.message
      } finally {
        this.submitting = false
      }
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

.refresh-button {
  padding: 14px 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, #39d0a4, #2b7fff);
  border: none;
  font-weight: bold;
  cursor: pointer;
}

.error-banner,
.inline-error {
  background: rgba(255, 122, 122, 0.2);
  padding: 14px;
  border-radius: 18px;
  margin: 20px 0;
  color: #ffd4d4;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin: 20px 0;
}

.summary-card {
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

.content-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 20px;
  margin: 20px 0;
}

.panel {
  background: var(--panel);
  border-radius: 24px;
  padding: 22px;
  border: 1px solid var(--line);
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

.quote-form span {
  display: block;
  margin-bottom: 8px;
}

.quote-form input,
.form-group input {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 14px;
  border-radius: 14px;
  color: #fff;
}

.quote-form button {
  padding: 14px 22px;
  border-radius: 999px;
  background: linear-gradient(90deg, #39d0a4, #2b7fff);
  border: none;
  font-weight: bold;
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

.tables-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
  margin: 20px 0;
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
}

.action-cell {
  white-space: nowrap;
}

.portfolio-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.portfolio-item {
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 18px;
}

.portfolio-row,
.portfolio-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.text-rise {
  color: #39d0a4;
}

.text-fall {
  color: #ff7a7a;
}

.action-button,
.sell-btn {
  padding: 8px 14px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  margin-right: 8px;
}

.action-button {
  background: #39d0a4;
  color: #000;
  font-weight: bold;
}

.sell-btn,
.sell-submit {
  background: #ff7a7a;
  color: #fff;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content {
  background: #12253f;
  border-radius: 24px;
  width: 500px;
  overflow: hidden;
}

.small-modal {
  width: 400px;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.close-btn {
  background: none;
  border: none;
  color: #fff;
  font-size: 20px;
  cursor: pointer;
}

.modal-body {
  padding: 20px;
  text-align: center;
}

.modal-footer {
  padding: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--line);
}

.modal-footer button {
  padding: 8px 16px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
}

.asset-form {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

@media (max-width: 1200px) {
  .summary-grid,
  .content-grid,
  .tables-grid,
  .quote-metrics {
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
  .quote-metrics {
    grid-template-columns: 1fr;
  }

  .hero {
    display: block;
  }

  .quote-form {
    flex-direction: column;
  }
}
</style>
