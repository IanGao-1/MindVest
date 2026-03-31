<template>
  <div id="app" class="app-shell">
    <section class="hero">
      <div>
        <p class="eyebrow">Financial Management Dashboard</p>
        <h1>资产、组合与实时行情总览</h1>
        <p class="hero-copy">
          前端直接对接你的 Spring Boot 接口，展示资产清单、投资组合和单只股票行情，并用 ECharts 做可视化。
        </p>
      </div>
      <button class="refresh-button" :disabled="loading" @click="loadDashboard">
        {{ loading ? '加载中...' : '刷新数据' }}
      </button>
    </section>

    <section v-if="error" class="error-banner">
      {{ error }}
    </section>

    <section class="summary-grid">
      <article class="summary-card accent-card">
        <span class="summary-label">资产总数</span>
        <strong>{{ assets.length }}</strong>
        <small>当前纳入系统管理的资产条目</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">组合总数</span>
        <strong>{{ portfolios.length }}</strong>
        <small>投资组合数量</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">资产市值估算</span>
        <strong>{{ formatCurrency(totalMarketValue) }}</strong>
        <small>按 currentPrice × quantity 汇总</small>
      </article>
      <article class="summary-card">
        <span class="summary-label">平均收益率</span>
        <strong :class="portfolioReturnClass">{{ formatPercent(averageReturnRate) }}</strong>
        <small>以买入价和现价估算</small>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel quote-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">行情查询</p>
            <h2>Yahoo Finance / Fallback</h2>
          </div>
        </div>

        <form class="quote-form" @submit.prevent="searchQuote">
          <label>
            <span>Ticker</span>
            <input v-model.trim="tickerInput" type="text" placeholder="例如 AAPL 或 0700.HK">
          </label>
          <button type="submit" :disabled="quoteLoading">
            {{ quoteLoading ? '查询中...' : '查询行情' }}
          </button>
        </form>

        <div v-if="quoteError" class="inline-error">{{ quoteError }}</div>

        <div v-if="quote" class="quote-metrics">
          <div class="metric-box">
            <span>价格</span>
            <strong>{{ formatCurrency(quote.price, quote.currency) }}</strong>
          </div>
          <div class="metric-box">
            <span>涨跌</span>
            <strong :class="quoteTrendClass">{{ formatSigned(quote.change) }}</strong>
          </div>
          <div class="metric-box">
            <span>来源</span>
            <strong>{{ quote.source || 'unknown' }}</strong>
          </div>
          <div class="metric-box">
            <span>时间</span>
            <strong>{{ quote.latestTimestamp || '实时返回' }}</strong>
          </div>
        </div>
        <div ref="quoteChart" class="chart chart-medium"></div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">分布可视化</p>
            <h2>资产类型占比</h2>
          </div>
        </div>
        <div ref="typeChart" class="chart"></div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">组合洞察</p>
            <h2>组合价值排名</h2>
          </div>
        </div>
        <div ref="portfolioChart" class="chart"></div>
      </article>
    </section>

    <section class="tables-grid">
      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">资产明细</p>
            <h2>Assets</h2>
          </div>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Ticker</th>
                <th>名称</th>
                <th>类型</th>
                <th>数量</th>
                <th>买入价</th>
                <th>现价</th>
                <th>估算市值</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="asset in assets" :key="asset.id">
                <td>{{ asset.ticker }}</td>
                <td>{{ asset.name }}</td>
                <td>{{ asset.type }}</td>
                <td>{{ formatNumber(asset.quantity) }}</td>
                <td>{{ formatCurrency(asset.buyPrice) }}</td>
                <td>{{ formatCurrency(asset.currentPrice) }}</td>
                <td>{{ formatCurrency(assetValue(asset)) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>

      <article class="panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">组合明细</p>
            <h2>Portfolios</h2>
          </div>
        </div>
        <div class="portfolio-list">
          <div v-for="portfolio in portfolios" :key="portfolio.id" class="portfolio-item">
            <div class="portfolio-row">
              <strong>{{ portfolio.name }}</strong>
              <span>{{ formatCurrency(portfolio.totalValue) }}</span>
            </div>
            <p>{{ portfolio.description || '暂无描述' }}</p>
            <div class="portfolio-meta">
              <span>{{ portfolio.assetCount || 0 }} 项资产</span>
              <span>{{ formatDate(portfolio.createdAt) }}</span>
            </div>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { fetchAssets, fetchPortfolios, fetchQuote } from './services/api'

export default {
  name: 'App',
  data() {
    return {
      assets: [],
      portfolios: [],
      quote: null,
      tickerInput: 'TSLA',
      loading: false,
      quoteLoading: false,
      error: '',
      quoteError: '',
      charts: {
        type: null,
        portfolio: null,
        quote: null
      }
    }
  },
  computed: {
    totalMarketValue() {
      return this.assets.reduce((sum, asset) => sum + this.assetValue(asset), 0)
    },
    averageReturnRate() {
      const validAssets = this.assets.filter(asset => asset.buyPrice && asset.currentPrice)
      if (!validAssets.length) {
        return 0
      }
      const totalRate = validAssets.reduce((sum, asset) => {
        return sum + ((asset.currentPrice - asset.buyPrice) / asset.buyPrice)
      }, 0)
      return totalRate / validAssets.length
    },
    portfolioReturnClass() {
      return this.averageReturnRate >= 0 ? 'text-rise' : 'text-fall'
    },
    quoteTrendClass() {
      if (!this.quote || this.quote.change == null) {
        return ''
      }
      return this.quote.change >= 0 ? 'text-rise' : 'text-fall'
    }
  },
  mounted() {
    this.loadDashboard()
    window.addEventListener('resize', this.handleResize)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize)
    Object.values(this.charts).forEach(chart => {
      if (chart) {
        chart.dispose()
      }
    })
  },
  methods: {
    async loadDashboard() {
      this.loading = true
      this.error = ''
      try {
        const [assets, portfolios] = await Promise.all([
          fetchAssets(),
          fetchPortfolios()
        ])
        this.assets = Array.isArray(assets) ? assets : []
        this.portfolios = Array.isArray(portfolios) ? portfolios : []
        this.$nextTick(() => {
          this.renderTypeChart()
          this.renderPortfolioChart()
        })
        if (!this.quote && this.tickerInput) {
          await this.searchQuote()
        }
      } catch (error) {
        this.error = `加载仪表盘失败：${error.message || '请确认后端已启动'}`
      } finally {
        this.loading = false
      }
    },
    async searchQuote() {
      if (!this.tickerInput) {
        this.quoteError = '请输入 ticker'
        return
      }
      this.quoteLoading = true
      this.quoteError = ''
      try {
        this.quote = await fetchQuote(this.tickerInput)
        this.$nextTick(() => {
          this.renderQuoteChart()
        })
      } catch (error) {
        this.quote = null
        this.quoteError = `行情获取失败：${error.message || '未知错误'}`
      } finally {
        this.quoteLoading = false
      }
    },
    assetValue(asset) {
      const price = Number(asset.currentPrice || 0)
      const quantity = Number(asset.quantity || 0)
      return price * quantity
    },
    formatCurrency(value, currency = 'USD') {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      const numeric = Number(value)
      try {
        return new Intl.NumberFormat('zh-CN', {
          style: 'currency',
          currency,
          maximumFractionDigits: 2
        }).format(numeric)
      } catch (error) {
        return numeric.toFixed(2)
      }
    },
    formatPercent(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      return `${(Number(value) * 100).toFixed(2)}%`
    },
    formatNumber(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      return Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    },
    formatSigned(value) {
      if (value == null || Number.isNaN(Number(value))) {
        return '--'
      }
      const numeric = Number(value)
      return `${numeric >= 0 ? '+' : ''}${numeric.toFixed(2)}`
    },
    formatDate(value) {
      if (!value) {
        return '--'
      }
      return new Date(value).toLocaleDateString('zh-CN')
    },
    getChartInstance(key, element) {
      if (!element) {
        return null
      }
      if (!this.charts[key]) {
        this.charts[key] = echarts.init(element)
      }
      return this.charts[key]
    },
    renderTypeChart() {
      const chart = this.getChartInstance('type', this.$refs.typeChart)
      if (!chart) {
        return
      }
      const grouped = this.assets.reduce((accumulator, asset) => {
        const type = asset.type || 'UNKNOWN'
        accumulator[type] = (accumulator[type] || 0) + this.assetValue(asset)
        return accumulator
      }, {})
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'item' },
        legend: {
          bottom: 0,
          textStyle: { color: '#d8e1ff' }
        },
        series: [
          {
            type: 'pie',
            radius: ['42%', '72%'],
            itemStyle: {
              borderRadius: 12,
              borderColor: '#07111f',
              borderWidth: 4
            },
            label: { color: '#eef4ff' },
            data: Object.keys(grouped).map(name => ({
              name,
              value: Number(grouped[name].toFixed(2))
            }))
          }
        ]
      })
    },
    renderPortfolioChart() {
      const chart = this.getChartInstance('portfolio', this.$refs.portfolioChart)
      if (!chart) {
        return
      }
      const portfolios = [...this.portfolios]
        .sort((a, b) => (b.totalValue || 0) - (a.totalValue || 0))
        .slice(0, 8)
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        grid: {
          left: 48,
          right: 18,
          top: 18,
          bottom: 48
        },
        xAxis: {
          type: 'category',
          data: portfolios.map(item => item.name),
          axisLabel: {
            color: '#c8d4f0',
            interval: 0,
            rotate: 20
          },
          axisLine: { lineStyle: { color: '#395175' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#c8d4f0' },
          splitLine: { lineStyle: { color: 'rgba(127, 156, 207, 0.16)' } }
        },
        series: [
          {
            type: 'bar',
            data: portfolios.map(item => Number((item.totalValue || 0).toFixed(2))),
            barWidth: 28,
            itemStyle: {
              borderRadius: [10, 10, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#6ce5b1' },
                { offset: 1, color: '#2b7fff' }
              ])
            }
          }
        ]
      })
    },
    renderQuoteChart() {
      const chart = this.getChartInstance('quote', this.$refs.quoteChart)
      if (!chart || !this.quote) {
        return
      }
      const indicators = [
        { label: 'Open', value: this.quote.open },
        { label: 'Prev Close', value: this.quote.previousClose },
        { label: 'Low', value: this.quote.dayLow },
        { label: 'High', value: this.quote.dayHigh },
        { label: 'Price', value: this.quote.price }
      ]
      chart.setOption({
        backgroundColor: 'transparent',
        tooltip: { trigger: 'axis' },
        grid: {
          left: 48,
          right: 18,
          top: 18,
          bottom: 36
        },
        xAxis: {
          type: 'category',
          data: indicators.map(item => item.label),
          axisLabel: { color: '#c8d4f0' },
          axisLine: { lineStyle: { color: '#395175' } }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#c8d4f0' },
          splitLine: { lineStyle: { color: 'rgba(127, 156, 207, 0.16)' } }
        },
        series: [
          {
            type: 'line',
            smooth: true,
            symbolSize: 10,
            data: indicators.map(item => Number(item.value || 0)),
            lineStyle: {
              width: 4,
              color: '#ffa94d'
            },
            itemStyle: {
              color: '#ffd166'
            },
            areaStyle: {
              color: 'rgba(255, 169, 77, 0.16)'
            }
          }
        ]
      })
    },
    handleResize() {
      Object.values(this.charts).forEach(chart => {
        if (chart) {
          chart.resize()
        }
      })
    }
  }
}
</script>

<style>
:root {
  --bg: #06101d;
  --panel: rgba(10, 23, 42, 0.88);
  --panel-strong: rgba(14, 31, 54, 0.96);
  --line: rgba(126, 156, 199, 0.2);
  --text: #eef4ff;
  --muted: #9db1d0;
  --accent: #39d0a4;
  --accent-2: #2b7fff;
  --danger: #ff7a7a;
}

* {
  box-sizing: border-box;
}

body {
  margin: 0;
  background:
    radial-gradient(circle at top left, rgba(43, 127, 255, 0.16), transparent 32%),
    radial-gradient(circle at top right, rgba(57, 208, 164, 0.14), transparent 26%),
    linear-gradient(180deg, #07111f 0%, #081522 100%);
  color: var(--text);
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

button,
input {
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
  padding: 28px 30px;
  border: 1px solid var(--line);
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(12, 31, 57, 0.95), rgba(6, 18, 32, 0.9));
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
}

.eyebrow,
.panel-kicker,
.summary-label {
  margin: 0 0 12px;
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero h1,
.panel h2 {
  margin: 0;
}

.hero h1 {
  font-size: clamp(30px, 5vw, 54px);
  line-height: 1.05;
  max-width: 620px;
}

.hero-copy {
  max-width: 680px;
  color: var(--muted);
  line-height: 1.7;
  margin: 16px 0 0;
}

.refresh-button,
.quote-form button {
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent), var(--accent-2));
  color: #04101a;
  font-weight: 800;
  padding: 14px 22px;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.refresh-button:hover,
.quote-form button:hover {
  transform: translateY(-1px);
}

.refresh-button:disabled,
.quote-form button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-banner,
.inline-error {
  margin-top: 20px;
  border: 1px solid rgba(255, 122, 122, 0.35);
  background: rgba(105, 22, 33, 0.35);
  color: #ffd4d4;
  padding: 14px 16px;
  border-radius: 18px;
}

.summary-grid,
.content-grid,
.tables-grid {
  display: grid;
  gap: 20px;
  margin-top: 22px;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.content-grid {
  grid-template-columns: 1.2fr 1fr 1fr;
}

.tables-grid {
  grid-template-columns: 1.4fr 1fr;
}

.summary-card,
.panel {
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--panel);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.18);
}

.summary-card {
  padding: 22px;
}

.summary-card strong {
  display: block;
  font-size: 34px;
  margin-bottom: 8px;
}

.summary-card small {
  color: var(--muted);
}

.accent-card {
  background: linear-gradient(135deg, rgba(57, 208, 164, 0.14), rgba(43, 127, 255, 0.12)), var(--panel-strong);
}

.panel {
  padding: 22px;
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
  align-items: end;
  margin-bottom: 18px;
}

.quote-form label {
  flex: 1;
}

.quote-form span {
  display: block;
  color: var(--muted);
  margin-bottom: 8px;
}

.quote-form input {
  width: 100%;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  padding: 14px 16px;
  color: var(--text);
}

.quote-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.metric-box {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 18px;
  padding: 14px;
}

.metric-box span,
.portfolio-item p,
.portfolio-meta,
th {
  color: var(--muted);
}

.metric-box strong {
  display: block;
  margin-top: 8px;
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
  padding: 14px 12px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.07);
}

td {
  color: #eff5ff;
}

.portfolio-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.portfolio-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.portfolio-row,
.portfolio-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.portfolio-item p {
  margin: 10px 0 12px;
  line-height: 1.6;
}

.text-rise {
  color: var(--accent);
}

.text-fall {
  color: var(--danger);
}

@media (max-width: 1200px) {
  .summary-grid,
  .content-grid,
  .tables-grid,
  .quote-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .app-shell {
    padding: 16px;
  }

  .hero,
  .quote-form,
  .summary-grid,
  .content-grid,
  .tables-grid,
  .quote-metrics {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .hero {
    display: flex;
  }

  .quote-form {
    display: flex;
    align-items: stretch;
  }

  .summary-card strong {
    font-size: 28px;
  }
}
</style>
