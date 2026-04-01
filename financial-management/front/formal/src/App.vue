<template>
  <div id="app" class="ledger-shell">
    <aside class="editorial-sidebar">
      <div class="sidebar-brand">
        <div class="sidebar-mark">
          <img src="/huifeng.png" alt="Huifeng logo" class="sidebar-logo">
        </div>
        <div>
          <h2>Huifeng Capital</h2>
          <p>Premium Tracker</p>
        </div>
      </div>

      <nav class="sidebar-nav">
        <a class="sidebar-link is-active" href="#overview">
          <span class="nav-icon nav-icon-dashboard"></span>
          <span>Dashboard</span>
        </a>
        <a class="sidebar-link" href="#holdings">
          <span class="nav-icon nav-icon-holdings"></span>
          <span>Holdings</span>
        </a>
        <a class="sidebar-link" href="#analytics">
          <span class="nav-icon nav-icon-analysis"></span>
          <span>Analysis</span>
        </a>
      </nav>

      <div class="sidebar-actions">
        <button class="sidebar-primary" @click="openTransactionModal">
          Add Transaction
        </button>
        <button class="sidebar-primary" :disabled="resettingSampleData || loading" @click="handleResetSampleData">
          {{ resettingSampleData ? 'Resetting...' : 'Reset Sample Data' }}
        </button>
        <button class="sidebar-secondary" :disabled="loading" @click="loadDashboard">
          {{ loading ? 'Loading...' : 'Refresh Dashboard' }}
        </button>
        <div class="sidebar-footlinks">
          <a href="#analytics" class="sidebar-footlink">
            <span class="nav-icon nav-icon-support"></span>
            <span>Support</span>
          </a>
          <a href="#overview" class="sidebar-footlink">
            <span class="nav-icon nav-icon-logout"></span>
            <span>Logout</span>
          </a>
        </div>
      </div>
    </aside>

    <main class="editorial-main">
      <header class="editorial-topbar">
        <div class="topbar-brand">
          <span class="wordmark">The Editorial Ledger</span>
          <nav class="topbar-nav">
            <a href="#overview" class="topbar-link is-active">Overview</a>
            <a href="#holdings" class="topbar-link">Holdings</a>
            <a href="#analytics" class="topbar-link">Research</a>
          </nav>
        </div>

        <div class="topbar-tools">
          <button class="tool-icon tool-icon-bell" type="button" aria-label="Notifications"></button>
          <button class="tool-icon tool-icon-settings" type="button" aria-label="Settings"></button>
          <div class="profile-chip">HF</div>
        </div>
      </header>

      <section id="overview" class="editorial-canvas">
        <section class="hero-ledger">
          <div>
            <span class="section-tag">Executive Summary</span>
            <h1>Total Equity Value</h1>
            <div class="hero-value-row">
              <div class="hero-value">
                <span class="hero-currency">{{ totalMarketValueParts.symbol }}</span>
                <span>{{ totalMarketValueParts.amount }}</span>
              </div>
              <div class="hero-pill" :class="holdingValueClass(totalPnL)">
                {{ formatSigned(totalPnL) }}
              </div>
            </div>
            <p class="hero-note">
              Cost basis {{ formatCurrency(totalCostBasis) }} across {{ assets.length }} active holdings.
            </p>
          </div>
        </section>

        <section v-if="error" class="error-banner">
          {{ error }}
        </section>

        <section class="bento-grid">
          <article class="editorial-card chart-card chart-span">
            <div class="card-header">
              <div>
                <span class="section-tag">Performance</span>
                <h2>Total Holdings P/L Trend</h2>
                <p>Unrealized performance curve across your live holdings.</p>
              </div>
              <div class="range-toggle">
                <button
                  type="button"
                  :class="['range-toggle-button', { 'is-active': portfolioTrendRange === '1M' }]"
                  @click="setPortfolioTrendRange('1M')"
                >
                  1M
                </button>
                <button
                  type="button"
                  :class="['range-toggle-button', { 'is-active': portfolioTrendRange === '6M' }]"
                  @click="setPortfolioTrendRange('6M')"
                >
                  6M
                </button>
              </div>
            </div>
            <div ref="portfolioPnlChart" class="chart chart-large"></div>
          </article>

          <article class="editorial-card allocation-card">
            <div class="card-header">
              <div>
                <span class="section-tag">Allocation</span>
                <h2>Asset Allocation</h2>
              </div>
            </div>
            <div ref="typeChart" class="chart chart-donut"></div>
            <div class="allocation-legend">
              <div v-for="item in allocationBreakdown" :key="item.name" class="allocation-legend-row">
                <div class="allocation-legend-label">
                  <span class="allocation-dot" :style="{ backgroundColor: item.color }"></span>
                  <strong>{{ item.name }}</strong>
                </div>
                <span>{{ item.percentLabel }}</span>
              </div>
            </div>
          </article>
        </section>

        <section id="holdings" class="holdings-row">
          <article class="editorial-card holdings-card">
            <div class="card-header holdings-header">
              <div>
                <span class="section-tag">Holdings Ledger</span>
                <h2>Current Holdings</h2>
              </div>
              <div class="holdings-summary">
                <span>{{ assets.length }} active rows</span>
                <span>{{ formatCurrency(totalPnL) }} unrealized</span>
              </div>
            </div>

            <div class="ledger-table-wrap">
              <table class="ledger-table">
                <thead>
                  <tr>
                    <th>Asset Name</th>
                    <th>Ticker</th>
                    <th>Type</th>
                    <th>Quantity</th>
                    <th>Avg Cost</th>
                    <th>Current Price</th>
                    <th>Market Value</th>
                    <th>P/L</th>
                    <th>Trend</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="asset in assets"
                    :key="asset.id"
                    class="holding-row"
                    @click="openAssetDetails(asset)"
                  >
                    <td class="asset-cell">
                      <div class="asset-row-meta">
                        <div class="asset-symbol">{{ asset.ticker.slice(0, 1) }}</div>
                        <div>
                          <strong>{{ asset.name }}</strong>
                        </div>
                      </div>
                    </td>
                    <td class="ticker-cell">{{ asset.ticker }}</td>
                    <td>{{ asset.type }}</td>
                    <td>{{ formatNumber(asset.quantity) }}</td>
                    <td>{{ formatCurrency(asset.avgCost) }}</td>
                    <td>{{ formatCurrency(asset.currentPrice) }}</td>
                    <td>{{ formatCurrency(asset.currentValue) }}</td>
                    <td :class="holdingValueClass(asset.unrealizedPnL)">
                      {{ formatCurrency(asset.unrealizedPnL) }}
                    </td>
                    <td class="trend-cell">
                      <div
                        v-if="getHoldingTrendSeries(asset.ticker, '1M').length"
                        class="trend-hover-card"
                        @mouseenter="showHoldingTrendPopover(asset, $event)"
                        @mouseleave="hideHoldingTrendPopover"
                      >
                        <svg class="sparkline" viewBox="0 0 96 28" preserveAspectRatio="none">
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

        <section class="activity-row-section">
          <article class="editorial-card compact-card activity-card">
            <div class="card-header">
              <div>
                <span class="section-tag">Recent Activity</span>
                <h2>Transaction Ledger</h2>
              </div>
            </div>
            <div class="activity-list">
              <div v-for="transaction in transactions.slice(0, 5)" :key="transaction.id" class="activity-row">
                <div class="activity-grid">
                  <div class="activity-cell">
                    <span>Date</span>
                    <strong>{{ formatDateTime(transaction.transactionDate) }}</strong>
                  </div>
                  <div class="activity-cell">
                    <span>Ticker</span>
                    <strong>{{ transaction.ticker }}</strong>
                  </div>
                  <div class="activity-cell">
                    <span>Asset</span>
                    <strong>{{ transaction.assetName || transaction.ticker }}</strong>
                  </div>
                  <div class="activity-cell">
                    <span>Quantity</span>
                    <strong>{{ formatNumber(transaction.quantity) }} sh</strong>
                  </div>
                  <div class="activity-cell activity-amount">
                    <span>Amount</span>
                    <strong :class="holdingValueClass(transaction.transactionType === 'BUY' ? 1 : -1)">
                      {{ formatCurrency(transaction.totalAmount) }}
                    </strong>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </section>

        <section id="analytics" class="analysis-grid">
          <article class="editorial-card chart-card">
            <div class="card-header">
              <div>
                <span class="section-tag">Research</span>
                <h2>Historical Price Explorer</h2>
              </div>
              <div class="research-controls">
                <form class="card-search" @submit.prevent="handleHistorySearch">
                  <input
                    v-model.trim="historyTickerInput"
                    type="text"
                    list="available-asset-options"
                    placeholder="Search ticker..."
                  >
                  <button type="submit" :disabled="historyLoading">
                    {{ historyLoading ? '...' : 'Search' }}
                  </button>
                </form>
                <label class="metric-selector">
                  <span>View</span>
                  <select v-model="historyMetric">
                    <option value="open">Open</option>
                    <option value="price">Close</option>
                    <option value="previousClose">PC</option>
                    <option value="volume">Volume</option>
                    <option value="dayHigh">High</option>
                    <option value="dayLow">Low</option>
                  </select>
                </label>
                <button class="hero-outline research-refresh" type="button" @click="handleHistorySearch" :disabled="historyLoading">
                  {{ historyLoading ? 'Loading...' : 'Refresh Research' }}
                </button>
              </div>
            </div>

            <form class="history-toolbar" @submit.prevent="handleHistorySearch">
              <label>
                <span>Range</span>
                <select v-model="historyRange">
                  <option value="1M">1 Month</option>
                  <option value="6M">6 Months</option>
                </select>
              </label>
            </form>

            <div v-if="historyError" class="inline-error">{{ historyError }}</div>
            <div v-else-if="selectedHistoryMeta" class="history-summary editorial-summary">
              <div class="mini-metric">
                <span>Ticker</span>
                <strong>{{ selectedHistoryMeta.symbol }}</strong>
              </div>
              <div class="mini-metric">
                <span>Name</span>
                <strong>{{ selectedHistoryMeta.name }}</strong>
              </div>
              <div class="mini-metric">
                <span>Asset Type</span>
                <strong>{{ selectedHistoryMeta.assetType }}</strong>
              </div>
              <div class="mini-metric">
                <span>Latest Price</span>
                <strong>{{ formatCurrency(selectedHistoryMeta.latestPrice) }}</strong>
              </div>
              <div class="mini-metric">
                <span>Current Price</span>
                <strong>{{ formatCurrency(selectedHistoryMeta.currentPrice) }}</strong>
              </div>
              <div class="mini-metric">
                <span>{{ selectedHistoryMetricLabel }}</span>
                <strong>{{ formatHistoryMetricValue(historyMetric, selectedHistoryMetricValue) }}</strong>
              </div>
              <div class="mini-metric mini-metric-wide">
                <span>Last Refreshed</span>
                <strong>{{ selectedHistoryMeta.lastRefreshed }}</strong>
              </div>
            </div>
            <div ref="portfolioHistoryChart" class="chart chart-large"></div>
          </article>
        </section>

        <section class="insight-grid">
          <article class="editorial-card insight-card">
            <span class="section-tag">Allocation Note</span>
            <h3>Capital is concentrated in your highest-conviction names.</h3>
            <p>Use the holdings ledger to rebalance sizing if one position dominates current value too aggressively.</p>
          </article>
          <article class="editorial-card insight-card insight-card-primary">
            <span class="section-tag">Market Insight</span>
            <h3>Live marks now refresh from Finnhub every 5 minutes.</h3>
            <p>Current price, current value, and unrealized P/L update from the latest cached quote snapshot.</p>
          </article>
          <article class="editorial-card insight-card">
            <span class="section-tag">Quick Action</span>
            <h3>Record the next ledger entry from the left rail.</h3>
            <p>The transaction form now opens as a modal so the dashboard stays clean and editorial.</p>
          </article>
        </section>

      </section>

      <div
        v-if="hoveredTrend"
        class="trend-floating-popover"
        :style="hoveredTrend.style"
      >
        <div class="trend-popover-header">
          <strong>{{ hoveredTrend.ticker }}</strong>
          <span>1 Month P/L</span>
        </div>
        <svg class="sparkline sparkline-large" viewBox="0 0 180 72" preserveAspectRatio="none">
          <path
            :d="sparklinePath(hoveredTrend.series, 180, 72)"
            :class="['sparkline-path', holdingValueClass(hoveredTrend.delta)]"
          />
        </svg>
        <div class="trend-popover-meta">
          <span>{{ formatCurrency(hoveredTrend.firstValue) }}</span>
          <span :class="holdingValueClass(hoveredTrend.delta)">{{ formatSigned(hoveredTrend.delta) }}</span>
          <span>{{ formatCurrency(hoveredTrend.lastValue) }}</span>
        </div>
      </div>
    </main>

    <div v-if="isTransactionModalOpen" class="transaction-modal-backdrop" @click.self="closeTransactionModal">
      <section class="transaction-modal">
        <div class="transaction-modal-head">
          <div>
            <span class="section-tag">Ledger Entry</span>
            <h2>New Transaction</h2>
          </div>
          <div class="transaction-modal-actions">
            <div class="entry-ref">
              <span>Reference</span>
              <strong>TXN-EL-{{ String(transactions.length + 1).padStart(4, '0') }}</strong>
            </div>
            <button class="modal-close" type="button" @click="closeTransactionModal">Close</button>
          </div>
        </div>

        <form class="editorial-form" @submit.prevent="submitTransaction">
          <div class="form-section">
            <div class="form-section-header">
              <h3>Asset Classification</h3>
              <p>Define the instrument for the ledger.</p>
            </div>
            <div class="form-grid">
              <label>
                <span>Transaction Type</span>
                <select v-model="transactionForm.transactionType">
                  <option value="BUY">BUY</option>
                  <option value="SELL">SELL</option>
                </select>
              </label>
              <label>
                <span>Asset Type</span>
                <input v-model.trim="transactionForm.assetType" type="text" placeholder="STOCK">
              </label>
              <label class="form-span-2">
                <span>Ticker / Asset Name</span>
                <input
                  v-model.trim="transactionForm.ticker"
                  type="text"
                  list="available-asset-options"
                  required
                  placeholder="AAPL"
                  @input="applyTickerDefaults"
                >
              </label>
              <label class="form-span-2">
                <span>Display Name</span>
                <input v-model.trim="transactionForm.assetName" type="text" placeholder="Apple Inc.">
              </label>
            </div>
          </div>

          <div class="form-section">
            <div class="form-section-header">
              <h3>Transaction Details</h3>
              <p>Precision entry for quantity and pricing.</p>
            </div>
            <div class="form-grid">
              <label>
                <span>Quantity</span>
                <input v-model.number="transactionForm.quantity" type="number" min="0.01" step="0.01" required>
              </label>
              <label>
                <span>Execution Price</span>
                <input v-model.number="transactionForm.price" type="number" min="0.01" step="0.01" required>
              </label>
              <label>
                <span>Current Price</span>
                <input v-model.number="transactionForm.currentPrice" type="number" min="0.01" step="0.01" placeholder="Auto-filled">
              </label>
              <label>
                <span>Date</span>
                <input
                  ref="transactionDateInput"
                  v-model="transactionForm.transactionDate"
                  type="datetime-local"
                  step="60"
                  @focus="openDatePicker"
                  @click="openDatePicker"
                >
              </label>
              <label class="form-span-2">
                <span>Notes</span>
                <textarea v-model.trim="transactionForm.notes" rows="3" placeholder="Optional notes"></textarea>
              </label>
            </div>
          </div>

          <div class="entry-preview">
            <div>
              <span>Estimated Initial Allocation</span>
              <strong>{{ formatCurrency((transactionForm.quantity || 0) * (transactionForm.price || 0)) }}</strong>
            </div>
            <div>
              <span>Current Mark</span>
              <strong>{{ formatCurrency(transactionForm.currentPrice) }}</strong>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" class="sidebar-secondary" @click="resetTransactionForm">Reset</button>
            <button type="submit" class="sidebar-primary" :disabled="submittingTransaction">
              {{ submittingTransaction ? 'Saving...' : 'Record Transaction' }}
            </button>
          </div>
        </form>
      </section>
    </div>

    <div v-if="selectedAsset" class="asset-detail-backdrop" @click.self="closeAssetDetails">
      <section class="asset-detail-modal">
        <div class="asset-detail-shell">
          <div class="asset-detail-main">
            <div class="asset-detail-head">
              <button class="detail-back" type="button" @click="closeAssetDetails">Back to Ledger</button>
              <span class="detail-ref">Investment Details</span>
            </div>

            <div class="asset-detail-hero">
              <div>
                <span class="section-tag">Selected Holding</span>
                <h2>{{ selectedAsset.name }}</h2>
                <p>{{ selectedAsset.ticker }} · {{ selectedAsset.type }}</p>
              </div>
              <div class="asset-detail-price">
                <strong>{{ formatCurrency(selectedAsset.currentPrice) }}</strong>
                <span :class="holdingValueClass(selectedAsset.unrealizedPnL)">
                  {{ formatSigned(selectedAsset.unrealizedPnL) }} unrealized
                </span>
              </div>
            </div>

            <article class="detail-chart-card">
              <div class="card-header">
                <div>
                  <span class="section-tag">Performance Since Entry</span>
                  <h3>P/L Curve Since Purchase</h3>
                </div>
              </div>
              <div v-if="selectedAssetTrendPoints.length" class="detail-spark-wrap">
                <div ref="detailPerformanceChart" class="chart detail-performance-chart"></div>
                <div class="detail-chart-meta">
                  <span>{{ formatCurrency(selectedAssetTrendFirstClose) }}</span>
                  <span :class="holdingValueClass(selectedAssetTrendDelta)">
                    {{ formatSigned(selectedAssetTrendDelta) }}
                  </span>
                  <span>{{ formatCurrency(selectedAssetTrendLastClose) }}</span>
                </div>
              </div>
              <div v-else class="detail-empty-state">
                No pricing curve available yet for this holding.
              </div>
            </article>

            <article class="detail-ledger-card">
              <div class="card-header">
                <div>
                  <span class="section-tag">Ledger Activity</span>
                  <h3>Transactions For {{ selectedAsset.ticker }}</h3>
                </div>
              </div>
              <div v-if="selectedAssetTransactions.length" class="detail-transaction-list">
                <div
                  v-for="transaction in selectedAssetTransactions"
                  :key="transaction.id"
                  class="detail-transaction-row"
                >
                  <div>
                    <strong>{{ transaction.transactionType }}</strong>
                    <span>{{ formatDateTime(transaction.transactionDate) }}</span>
                  </div>
                  <div class="detail-transaction-numbers">
                    <strong>{{ formatNumber(transaction.quantity) }} sh</strong>
                    <span>{{ formatCurrency(transaction.price) }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="detail-empty-state">
                No ledger entries recorded for this ticker yet.
              </div>
            </article>
          </div>

          <aside class="asset-detail-side">
            <article class="detail-side-card detail-side-card-primary">
              <span class="section-tag">Position</span>
              <h3>Your Position</h3>
              <div class="detail-metric-list">
                <div class="detail-metric">
                  <span>Market Value</span>
                  <strong>{{ formatCurrency(selectedAsset.currentValue) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>Cost Basis</span>
                  <strong>{{ formatCurrency(selectedAsset.costBasis) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>Quantity</span>
                  <strong>{{ formatNumber(selectedAsset.quantity) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>Avg Cost</span>
                  <strong>{{ formatCurrency(selectedAsset.avgCost) }}</strong>
                </div>
              </div>
            </article>

            <article class="detail-side-card">
              <span class="section-tag">Actions</span>
              <h3>Record Next Move</h3>
              <p>Open the ledger form with this holding already staged.</p>
              <div class="detail-action-stack">
                <button class="sidebar-primary" type="button" @click="startAssetTransaction">Add Transaction</button>
              </div>
            </article>

            <article class="detail-side-card">
              <span class="section-tag">Market Snapshot</span>
              <h3>Quote Details</h3>
              <div v-if="selectedAssetQuoteError" class="inline-error">{{ selectedAssetQuoteError }}</div>
              <div v-else-if="selectedAssetQuoteLoading" class="detail-empty-state">Loading quote...</div>
              <div v-else class="detail-metric-list">
                <div class="detail-metric">
                  <span>Open</span>
                  <strong>{{ formatCurrency(selectedAssetQuote?.open) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>Close</span>
                  <strong>{{ formatCurrency(selectedAssetQuote?.price) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>Volume</span>
                  <strong>{{ formatNumber(selectedAssetQuote?.volume) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>PC</span>
                  <strong>{{ formatCurrency(selectedAssetQuote?.previousClose) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>Low</span>
                  <strong>{{ formatCurrency(selectedAssetQuote?.dayLow) }}</strong>
                </div>
                <div class="detail-metric">
                  <span>High</span>
                  <strong>{{ formatCurrency(selectedAssetQuote?.dayHigh) }}</strong>
                </div>
              </div>
            </article>
          </aside>
        </div>
      </section>
    </div>

    <datalist id="available-asset-options">
      <option
        v-for="option in availableAssetOptions"
        :key="option.ticker"
        :value="option.ticker"
      >
        {{ option.label }}
      </option>
    </datalist>
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
  BND: { assetName: 'Vanguard Total Bond Market', assetType: 'BOND ETF' },
  C: { assetName: 'Citigroup Inc.', assetType: 'STOCK' },
  CNY: { assetName: 'Chinese Yuan', assetType: 'FOREX' },
  GLD: { assetName: 'SPDR Gold Shares', assetType: 'COMMODITY ETF' },
  IEF: { assetName: 'iShares 7-10 Year Treasury Bond', assetType: 'BOND ETF' },
  META: { assetName: 'Meta Platforms Inc.', assetType: 'STOCK' },
  MSFT: { assetName: 'Microsoft Corp.', assetType: 'STOCK' },
  NVDA: { assetName: 'NVIDIA Corp.', assetType: 'STOCK' },
  QQQ: { assetName: 'Invesco QQQ Trust', assetType: 'EQUITY ETF' },
  SLV: { assetName: 'iShares Silver Trust', assetType: 'COMMODITY ETF' },
  SPY: { assetName: 'SPDR S&P 500 ETF Trust', assetType: 'EQUITY ETF' },
  TLT: { assetName: 'iShares 20+ Year Treasury Bond', assetType: 'BOND ETF' },
  TSLA: { assetName: 'Tesla Inc.', assetType: 'STOCK' },
  USD: { assetName: 'US Dollar', assetType: 'FOREX' },
  USO: { assetName: 'United States Oil Fund', assetType: 'COMMODITY ETF' },
  VNQ: { assetName: 'Vanguard Real Estate ETF', assetType: 'REAL ESTATE ETF' },
  VOO: { assetName: 'Vanguard S&P 500 ETF', assetType: 'EQUITY ETF' }
}

const HISTORY_TICKER_ALIASES = {
  APPL: 'AAPL'
}

const ALLOCATION_COLORS = ['#a30113', '#45525b', '#d6dadd', '#0a6c74', '#d48806', '#7c4dff']

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
      historyMetric: 'open',
      portfolioTrendRange: '1M',
      historyLoading: false,
      historyError: '',
      selectedHistory: null,
      selectedHistoryMeta: null,
      selectedHistoryQuote: null,
      holdingTrendHistories: {},
      hoveredTrend: null,
      loading: false,
      quoteLoading: false,
      submittingTransaction: false,
      resettingSampleData: false,
      transactionQuoteLoading: false,
      isTransactionModalOpen: false,
      selectedAsset: null,
      selectedAssetQuote: null,
      selectedAssetQuoteLoading: false,
      selectedAssetQuoteError: '',
      error: '',
      quoteError: '',
      transactionForm: createEmptyTransactionForm(),
      charts: { type: null, quote: null, portfolioPnl: null, portfolioHistory: null, holdingsHistory: null, detailPerformance: null },
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
    totalMarketValueParts() {
      return this.formatCurrencyParts(this.totalMarketValue)
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
    },
    allocationBreakdown() {
      const totalValue = this.totalMarketValue || 0
      const grouped = this.assets.reduce((accumulator, asset) => {
        const assetType = asset.type || 'UNKNOWN'
        accumulator[assetType] = (accumulator[assetType] || 0) + Number(asset.currentValue || 0)
        return accumulator
      }, {})

      return Object.entries(grouped)
        .sort((first, second) => second[1] - first[1])
        .map(([name, value], index) => ({
          name,
          value,
          color: ALLOCATION_COLORS[index % ALLOCATION_COLORS.length],
          percentLabel: totalValue > 0 ? `${((value / totalValue) * 100).toFixed(1)}%` : '0.0%'
        }))
    },
    availableAssetOptions() {
      return Object.entries(TICKER_PRESETS)
        .sort((first, second) => first[0].localeCompare(second[0]))
        .map(([ticker, preset]) => ({
          ticker,
          label: `${ticker} — ${preset.assetName}`,
          assetName: preset.assetName,
          assetType: preset.assetType
        }))
    },
    selectedHistoryMetricLabel() {
      return {
        open: 'Open',
        price: 'Close',
        previousClose: 'PC',
        volume: 'Volume',
        dayHigh: 'High',
        dayLow: 'Low'
      }[this.historyMetric] || 'Metric'
    },
    selectedHistoryMetricValue() {
      if (!this.selectedHistoryMeta) {
        return null
      }
      return this.selectedHistoryMeta[this.historyMetric] ?? null
    },
    selectedAssetTransactions() {
      if (!this.selectedAsset?.ticker) {
        return []
      }
      return this.transactions
        .filter(transaction => this.normalizeHistoryTicker(transaction.ticker) === this.normalizeHistoryTicker(this.selectedAsset.ticker))
        .sort((first, second) => new Date(second.transactionDate) - new Date(first.transactionDate))
    },
    selectedAssetTrendPoints() {
      if (!this.selectedAsset?.ticker) {
        return []
      }
      return this.getHoldingTrendPoints(this.selectedAsset.ticker, '1M')
    },
    selectedAssetTrendSeries() {
      if (!this.selectedAsset?.ticker) {
        return []
      }
      return this.getHoldingTrendSeries(this.selectedAsset.ticker, '1M')
    },
    selectedAssetTrendDelta() {
      if (!this.selectedAsset?.ticker) {
        return 0
      }
      return this.getHoldingTrendDelta(this.selectedAsset.ticker, '1M')
    },
    selectedAssetTrendFirstClose() {
      if (!this.selectedAsset?.ticker) {
        return null
      }
      return this.getHoldingTrendFirstClose(this.selectedAsset.ticker, '1M')
    },
    selectedAssetTrendLastClose() {
      if (!this.selectedAsset?.ticker) {
        return null
      }
      return this.getHoldingTrendLastClose(this.selectedAsset.ticker, '1M')
    }
  },
  mounted() {
    this.loadDashboard()
    window.addEventListener('resize', this.handleResize)
  },
  watch: {
    historyMetric() {
      this.$nextTick(() => this.renderPortfolioHistoryChart())
    },
    historyRange() {
      if (!this.selectedHistory) {
        return
      }
      this.$nextTick(() => this.renderPortfolioHistoryChart())
    },
    selectedAsset() {
      this.$nextTick(() => this.renderDetailPerformanceChart())
    }
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
        this.closeTransactionModal()
        await this.loadDashboard()
      } catch (error) {
        this.error = `Transaction failed: ${error.message || 'Unknown error'}`
      } finally {
        this.submittingTransaction = false
      }
    },
    openTransactionModal(prefill = null) {
      this.isTransactionModalOpen = true
      if (!prefill) {
        return
      }
      this.transactionForm = {
        ...this.transactionForm,
        transactionType: prefill.transactionType || this.transactionForm.transactionType,
        ticker: prefill.ticker || this.transactionForm.ticker,
        assetName: prefill.assetName || this.transactionForm.assetName,
        assetType: prefill.assetType || this.transactionForm.assetType,
        currentPrice: prefill.currentPrice ?? this.transactionForm.currentPrice
      }
      this.applyTickerDefaults()
    },
    closeTransactionModal() {
      this.isTransactionModalOpen = false
    },
    async openAssetDetails(asset) {
      this.selectedAsset = asset
      this.selectedAssetQuote = null
      this.selectedAssetQuoteError = ''
      this.selectedAssetQuoteLoading = true
      try {
        this.selectedAssetQuote = await fetchQuote(asset.ticker)
      } catch (error) {
        this.selectedAssetQuoteError = `Quote failed: ${error.message || 'Unknown error'}`
      } finally {
        this.selectedAssetQuoteLoading = false
      }
    },
    closeAssetDetails() {
      this.charts.detailPerformance?.dispose()
      this.charts.detailPerformance = null
      this.selectedAsset = null
      this.selectedAssetQuote = null
      this.selectedAssetQuoteError = ''
      this.selectedAssetQuoteLoading = false
    },
    startAssetTransaction() {
      if (!this.selectedAsset) {
        return
      }
      this.closeAssetDetails()
      this.openTransactionModal({
        ticker: this.selectedAsset.ticker,
        assetName: this.selectedAsset.name,
        assetType: this.selectedAsset.type,
        currentPrice: this.selectedAsset.currentPrice
      })
    },
    setPortfolioTrendRange(range) {
      this.portfolioTrendRange = range
      this.$nextTick(() => this.renderPortfolioPnlChart())
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
      this.transactionForm.assetName = preset.assetName
      this.transactionForm.assetType = preset.assetType
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
    formatCurrencyParts(value) {
      const formatted = this.formatCurrency(value)
      if (formatted === '--') {
        return { symbol: '', amount: '--' }
      }
      const match = formatted.match(/^([^\d-]+)(.+)$/)
      if (!match) {
        return { symbol: '', amount: formatted }
      }
      return {
        symbol: match[1],
        amount: match[2]
      }
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
    formatHistoryMetricValue(metric, value) {
      if (metric === 'volume') {
        return this.formatNumber(value)
      }
      return this.formatCurrency(value)
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
      const colorMap = this.allocationBreakdown.reduce((accumulator, item) => {
        accumulator[item.name] = item.color
        return accumulator
      }, {})

      chart.setOption({
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            label: { show: false },
            labelLine: { show: false },
            emphasis: { scale: true },
            data: Object.entries(grouped).map(([name, value]) => ({
              name,
              value,
              itemStyle: {
                color: colorMap[name]
              }
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
        grid: {
          left: 70,
          right: 16,
          top: 20,
          bottom: 28
        },
        xAxis: { data: ['Open', 'Prev Close', 'Low', 'High', 'Price'] },
        yAxis: {
          type: 'value',
          min: quoteRange.min,
          max: quoteRange.max,
          axisLabel: {
            width: 56,
            overflow: 'truncate',
            formatter: value => this.formatCompactCurrency(value)
          }
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
    getHistoryMetricPointValue(points, index, metric) {
      const point = points[index]
      if (!point) {
        return null
      }
      if (metric === 'open') {
        return point.open
      }
      if (metric === 'volume') {
        return point.volume
      }
      if (metric === 'dayHigh') {
        return point.high
      }
      if (metric === 'dayLow') {
        return point.low
      }
      if (metric === 'previousClose') {
        return index > 0 ? points[index - 1].close : point.close
      }
      return point.close
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
      const metricSeries = historyPoints.map((point, index) => this.getHistoryMetricPointValue(historyPoints, index, this.historyMetric))
      const historyRange = this.buildChartRange(metricSeries)

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
                  <span style="color: ${valueColor}; font-weight: 700;">${this.formatHistoryMetricValue(this.historyMetric, numericValue)}</span>
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
            data: metricSeries
          }
        ]
      })
    },
    renderDetailPerformanceChart() {
      const chart = this.getChartInst('detailPerformance', this.$refs.detailPerformanceChart)
      if (!chart) {
        return
      }

      const points = this.selectedAssetTrendPoints
      if (!points.length) {
        chart.clear()
        return
      }

      const pnlSeries = points.map(point => Number(point.pnl || 0))
      const range = this.buildChartRange(pnlSeries)
      const latestPnl = pnlSeries[pnlSeries.length - 1] || 0
      const trendColor = latestPnl >= 0 ? '#db3f16' : '#8f3a2d'

      chart.setOption({
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => this.formatCurrency(value)
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
          data: points.map(point => point.date)
        },
        yAxis: {
          type: 'value',
          min: range.min,
          max: range.max,
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
    async handleHistorySearch() {
      this.historyLoading = true
      this.historyError = ''

      try {
        const normalizedTicker = this.normalizeHistoryTicker(this.historyTickerInput)
        const [history, quote] = await Promise.all([
          fetchHistoryJson(normalizedTicker),
          fetchQuote(normalizedTicker).catch(() => null)
        ])

        if (!history) {
          throw new Error(`No history response found for ${normalizedTicker}`)
        }

        this.historyTickerInput = normalizedTicker
        this.selectedHistory = history
        this.selectedHistoryQuote = quote
        const filteredPrices = this.getFilteredHistoryPrices(history.prices)
        const descriptor = this.getAssetDescriptor(normalizedTicker)

        if (!filteredPrices.length) {
          throw new Error(`No ${this.historyRangeLabel} data available for ${normalizedTicker}`)
        }

        const latestPoint = filteredPrices[filteredPrices.length - 1]
        this.selectedHistoryMeta = {
          symbol: normalizedTicker,
          name: descriptor.name,
          assetType: descriptor.assetType,
          open: quote?.open ?? latestPoint.open ?? null,
          price: quote?.price ?? Number(latestPoint.close || 0),
          previousClose: quote?.previousClose ?? null,
          volume: latestPoint.volume ?? null,
          dayHigh: quote?.dayHigh ?? latestPoint.high ?? null,
          dayLow: quote?.dayLow ?? latestPoint.low ?? null,
          currentPrice: quote?.price ?? Number(latestPoint.close || 0),
          lastRefreshed: history.lastRefreshed || latestPoint.date,
          latestPrice: Number(latestPoint.close || 0)
        }

        this.$nextTick(() => this.renderPortfolioHistoryChart())
      } catch (error) {
        this.selectedHistory = null
        this.selectedHistoryQuote = null
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
        open: point.open != null ? Number(point.open || 0) : null,
        high: point.high != null ? Number(point.high || 0) : null,
        low: point.low != null ? Number(point.low || 0) : null,
        close: Number(point.close || 0),
        volume: point.volume != null ? Number(point.volume || 0) : null
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
    getAssetSnapshot(ticker) {
      const normalizedTicker = this.normalizeHistoryTicker(ticker)
      return this.assets.find(asset => this.normalizeHistoryTicker(asset.ticker) === normalizedTicker) || null
    },
    getAssetDescriptor(ticker) {
      const asset = this.getAssetSnapshot(ticker)
      const preset = TICKER_PRESETS[this.normalizeHistoryTicker(ticker)] || null
      return {
        name: asset?.name || preset?.assetName || this.normalizeHistoryTicker(ticker),
        assetType: asset?.type || preset?.assetType || 'UNKNOWN'
      }
    },
    getAssetPurchaseDate(ticker) {
      const asset = this.getAssetSnapshot(ticker)
      if (!asset?.purchaseDate) {
        return null
      }
      const purchaseDate = new Date(asset.purchaseDate)
      return Number.isNaN(purchaseDate.getTime()) ? null : purchaseDate
    },
    getHoldingTrendPoints(ticker, range = this.historyRange) {
      const asset = this.getAssetSnapshot(ticker)
      const normalizedTicker = this.normalizeHistoryTicker(ticker)
      const history = this.holdingTrendHistories[normalizedTicker]
      const purchaseDate = this.getAssetPurchaseDate(ticker)
      const quantity = Number(asset?.quantity || 0)
      const avgCost = Number(asset?.avgCost || 0)

      if (!asset || !history || quantity <= 0) {
        return []
      }

      const basePoints = this.getFilteredHistoryPrices(history?.prices || [], range)
      const filteredPoints = basePoints
        .filter(point => {
          if (!purchaseDate) {
            return true
          }
          return new Date(point.date) >= purchaseDate
        })

      const pointsToUse = filteredPoints.length ? filteredPoints : basePoints

      return pointsToUse
        .map(point => ({
          date: point.date,
          close: Number(point.close || 0),
          pnl: (Number(point.close || 0) - avgCost) * quantity
        }))
    },
    getPortfolioPnlTrendPoints(range = '1M') {
      const normalizedAssets = this.assets
        .map(asset => ({
          ticker: this.normalizeHistoryTicker(asset.ticker),
          quantity: Number(asset.quantity || 0),
          avgCost: Number(asset.avgCost || 0),
          purchaseDate: asset.purchaseDate ? new Date(asset.purchaseDate) : null
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
          .filter(point => {
            if (!asset.purchaseDate || Number.isNaN(asset.purchaseDate.getTime())) {
              return true
            }
            return new Date(point.date) >= asset.purchaseDate
          })
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

      const pnlPoints = this.getPortfolioPnlTrendPoints(this.portfolioTrendRange)
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
      return this.getHoldingTrendPoints(ticker, range).map(point => Number(point.pnl || 0))
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
        firstValue: this.getHoldingTrendFirstClose(asset.ticker, '1M'),
        lastValue: this.getHoldingTrendLastClose(asset.ticker, '1M'),
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
@import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;600;700;800&family=Work+Sans:wght@400;500;600;700&display=swap');

:root {
  --background: #f9f9f9;
  --surface: #ffffff;
  --surface-low: #f3f3f3;
  --surface-mid: #eeeeee;
  --text: #1a1c1c;
  --muted: #546067;
  --primary: #a30113;
  --primary-soft: #ffdad6;
  --outline: rgba(228, 190, 186, 0.4);
  --gain: #00557a;
  --loss: #ba1a1a;
}

* {
  box-sizing: border-box;
}

html {
  scroll-behavior: smooth;
}

body {
  margin: 0;
  background: var(--background);
  color: var(--text);
  font-family: 'Work Sans', sans-serif;
}

button,
input,
select,
textarea {
  font: inherit;
}

a {
  color: inherit;
  text-decoration: none;
}

.ledger-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  background: linear-gradient(180deg, #fbfbfb 0%, #f5f3f2 100%);
}

.editorial-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(24px);
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.sidebar-mark {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: var(--primary);
  display: grid;
  place-items: center;
}

.sidebar-logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.sidebar-brand h2,
.wordmark,
.hero-ledger h1,
.card-header h2,
.form-section-header h3,
.entry-heading h2 {
  font-family: 'Manrope', sans-serif;
}

.sidebar-brand h2 {
  margin: 0;
  font-size: 0.95rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-weight: 800;
}

.sidebar-brand p {
  margin: 2px 0 0;
  font-size: 0.7rem;
  color: var(--muted);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.sidebar-nav {
  display: grid;
  gap: 6px;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  color: var(--muted);
}

.sidebar-link.is-active {
  color: var(--primary);
  background: var(--surface-low);
}

.sidebar-actions {
  margin-top: auto;
  display: grid;
  gap: 12px;
}

.sidebar-footlinks {
  margin-top: 8px;
  padding-top: 14px;
  border-top: 1px solid rgba(228, 190, 186, 0.35);
  display: grid;
  gap: 6px;
}

.sidebar-footlink {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.nav-icon {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  background: var(--surface-mid);
  position: relative;
  flex: 0 0 auto;
}

.nav-icon-dashboard::before,
.nav-icon-holdings::before,
.nav-icon-analysis::before,
.nav-icon-support::before,
.nav-icon-logout::before {
  content: "";
  position: absolute;
  inset: 4px;
  border: 2px solid var(--muted);
}

.nav-icon-dashboard::before {
  inset: 3px;
  border: none;
  background:
    linear-gradient(var(--muted), var(--muted)) left top/5px 5px no-repeat,
    linear-gradient(var(--muted), var(--muted)) right top/5px 5px no-repeat,
    linear-gradient(var(--muted), var(--muted)) left bottom/5px 5px no-repeat,
    linear-gradient(var(--muted), var(--muted)) right bottom/5px 5px no-repeat;
}

.nav-icon-holdings::before {
  inset: 3px 4px;
  border-width: 2px 0;
  border-style: solid;
  border-color: var(--muted);
}

.nav-icon-analysis::before {
  inset: auto 3px 3px 3px;
  height: 10px;
  border: none;
  background:
    linear-gradient(var(--muted), var(--muted)) left bottom/3px 6px no-repeat,
    linear-gradient(var(--muted), var(--muted)) center bottom/3px 10px no-repeat,
    linear-gradient(var(--muted), var(--muted)) right bottom/3px 8px no-repeat;
}

.nav-icon-support::before {
  inset: 4px;
  border-radius: 50%;
}

.nav-icon-logout::before {
  inset: 5px 3px 5px 7px;
  border-left: none;
}

.topbar-tools {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tool-icon {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background: var(--surface-low);
  position: relative;
  cursor: pointer;
}

.tool-icon::before,
.tool-icon::after {
  content: "";
  position: absolute;
}

.tool-icon-bell::before {
  width: 12px;
  height: 10px;
  left: 12px;
  top: 9px;
  border: 2px solid var(--muted);
  border-bottom: none;
  border-radius: 8px 8px 0 0;
}

.tool-icon-bell::after {
  width: 10px;
  height: 2px;
  left: 13px;
  top: 20px;
  background: var(--muted);
  box-shadow: 3px 5px 0 -1px var(--muted);
}

.tool-icon-settings::before {
  inset: 10px;
  border: 2px solid var(--muted);
  border-radius: 50%;
}

.tool-icon-settings::after {
  inset: 6px;
  border-radius: 50%;
  background:
    linear-gradient(var(--muted), var(--muted)) center/2px 22px no-repeat,
    linear-gradient(var(--muted), var(--muted)) center/22px 2px no-repeat;
}

.profile-chip {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: #143948;
  color: #fff;
  font-family: 'Manrope', sans-serif;
  font-weight: 800;
  font-size: 0.78rem;
}

.sidebar-primary,
.hero-outline,
.history-toolbar button {
  border: none;
  background: linear-gradient(135deg, var(--primary), #c72628);
  color: #fff;
  padding: 14px 18px;
  border-radius: 0.375rem;
  font-family: 'Manrope', sans-serif;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.72rem;
  cursor: pointer;
}

.sidebar-secondary {
  border: none;
  background: var(--surface-mid);
  color: var(--text);
  padding: 14px 18px;
  border-radius: 0.375rem;
  font-family: 'Manrope', sans-serif;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  font-size: 0.72rem;
  cursor: pointer;
}

.editorial-main {
  min-width: 0;
}

.editorial-topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 24px;
  padding: 22px 40px;
  background: rgba(249, 249, 249, 0.82);
  backdrop-filter: blur(24px);
}

.topbar-brand {
  display: flex;
  align-items: center;
  gap: 28px;
}

.wordmark {
  font-weight: 800;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--primary);
}

.topbar-nav {
  display: flex;
  gap: 18px;
}

.topbar-link {
  font-size: 0.86rem;
  color: var(--muted);
}

.topbar-link.is-active {
  color: var(--primary);
  font-weight: 700;
}

.card-search {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--surface-low);
  padding: 8px 10px;
  border-radius: 999px;
}

.card-search input {
  border: none;
  background: transparent;
  min-width: 180px;
  color: var(--text);
}

.card-search button {
  border: none;
  background: transparent;
  color: var(--primary);
  font-weight: 700;
  cursor: pointer;
}

.editorial-canvas {
  max-width: 1440px;
  margin: 0 auto;
  padding: 40px;
}

.hero-ledger {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin-bottom: 40px;
}

.section-tag {
  display: block;
  margin-bottom: 10px;
  color: var(--primary);
  font-size: 0.66rem;
  font-weight: 800;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.hero-ledger h1 {
  margin: 0;
  font-size: 1.75rem;
  line-height: 1.02;
  letter-spacing: -0.03em;
}

.hero-value-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 10px;
}

.hero-value {
  font-family: 'Manrope', sans-serif;
  font-size: 4.5rem;
  font-weight: 800;
  letter-spacing: -0.06em;
}

.hero-currency {
  font-size: 0.48em;
  vertical-align: top;
  margin-right: 6px;
}

.hero-pill {
  padding: 8px 14px;
  border-radius: 999px;
  background: #d7e4ec;
  font-size: 0.78rem;
  font-weight: 700;
}

.hero-note {
  margin: 12px 0 0;
  color: var(--muted);
}

.hero-outline {
  background: var(--surface);
  color: var(--primary);
  box-shadow: 0 12px 32px rgba(163, 1, 19, 0.06);
}

.error-banner,
.inline-error {
  margin-bottom: 24px;
  background: var(--primary-soft);
  color: var(--primary);
  padding: 14px 16px;
  border-radius: 0.5rem;
}

.bento-grid,
.holdings-row,
.activity-row-section,
.analysis-grid,
.entry-grid {
  display: grid;
  gap: 24px;
  margin-bottom: 28px;
}

.bento-grid {
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.9fr);
}

.support-row {
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.95fr);
  align-items: start;
}

.editorial-card {
  background: var(--surface);
  padding: 28px;
  border-radius: 0.75rem;
  box-shadow: 0 24px 48px rgba(163, 1, 19, 0.04);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.card-header h2 {
  margin: 0;
  font-size: 1.35rem;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.card-header p,
.form-section-header p,
.holdings-summary,
.asset-cell span,
.activity-row span {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 0.9rem;
}

.chart {
  width: 100%;
}

.chart-large {
  height: 320px;
}

.chart-donut {
  height: 260px;
}

.chart-quote {
  height: 180px;
}

.allocation-legend,
.editorial-summary {
  display: grid;
  gap: 12px;
}

.allocation-legend {
  margin-top: 18px;
}

.editorial-summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.allocation-legend-row,
.allocation-legend-label {
  display: flex;
  align-items: center;
}

.allocation-legend-row {
  justify-content: space-between;
  gap: 18px;
  padding: 8px 0;
}

.allocation-legend-label {
  gap: 14px;
}

.allocation-legend-label strong,
.allocation-legend-row > span {
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  font-weight: 700;
}

.allocation-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.mini-metric {
  padding: 14px 16px;
  background: var(--surface-low);
}

.mini-metric span,
.entry-ref span {
  display: block;
  color: var(--muted);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.mini-metric strong,
.entry-ref strong {
  display: block;
  margin-top: 6px;
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  font-weight: 800;
}

.mini-metric-wide {
  grid-column: 1 / -1;
}

.ledger-table-wrap {
  overflow-x: auto;
}

.ledger-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.ledger-table th {
  padding: 16px 12px;
  text-align: left;
  font-size: 0.68rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--muted);
  background: var(--surface-low);
}

.ledger-table td {
  padding: 18px 12px;
  border-bottom: 10px solid var(--surface);
  background: #fcfcfc;
  vertical-align: middle;
}

.ledger-table tbody tr:nth-child(even) td {
  background: #f9f9f9;
}

.holding-row {
  cursor: pointer;
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.holding-row:hover td {
  background: #f7f1ef;
}

.holding-row:hover td:first-child {
  box-shadow: inset 3px 0 0 var(--primary);
}

.asset-cell strong {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 0.98rem;
}

.asset-row-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.asset-symbol {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  background: var(--surface-mid);
  color: var(--primary);
  font-family: 'Manrope', sans-serif;
  font-weight: 800;
}

.ticker-cell {
  font-family: 'Manrope', sans-serif;
  font-weight: 800;
  color: var(--primary);
}

.compact-card {
  padding: 24px;
}

.activity-card {
  min-height: 100%;
}

.range-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  background: var(--surface-low);
  border-radius: 999px;
}

.range-toggle-button {
  border: none;
  background: transparent;
  color: var(--muted);
  padding: 10px 14px;
  border-radius: 999px;
  font-family: 'Manrope', sans-serif;
  font-size: 0.76rem;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  cursor: pointer;
}

.range-toggle-button.is-active {
  background: #fff;
  color: var(--primary);
  box-shadow: 0 8px 24px rgba(163, 1, 19, 0.08);
}

.activity-list {
  display: grid;
  gap: 16px;
}

.activity-row {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(228, 190, 186, 0.2);
}

.activity-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr 1.3fr 0.9fr 1fr;
  gap: 18px;
  align-items: start;
}

.activity-cell {
  display: grid;
  gap: 6px;
}

.activity-row strong,
.activity-cell strong {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 1rem;
  line-height: 1.1;
}

.activity-amount {
  text-align: right;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
  margin-bottom: 28px;
}

.insight-card h3 {
  margin: 0 0 10px;
  font-family: 'Manrope', sans-serif;
  font-size: 1.25rem;
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.insight-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.65;
}

.insight-card-primary {
  background: linear-gradient(180deg, #006e9d 0%, #00557a 100%);
  color: #fff;
}

.insight-card-primary .section-tag,
.insight-card-primary p {
  color: rgba(255, 255, 255, 0.82);
}

.research-controls {
  display: flex;
  align-items: end;
  gap: 16px;
}

.research-refresh {
  white-space: nowrap;
}

.history-toolbar,
.form-grid {
  display: grid;
  gap: 18px;
}

.history-toolbar {
  grid-template-columns: 260px;
  align-items: end;
  margin-bottom: 16px;
}

.metric-selector {
  display: grid;
  gap: 8px;
  min-width: 140px;
}

.metric-selector span {
  font-size: 0.72rem;
  color: var(--muted);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.metric-selector select {
  border: none;
  border-bottom: 2px solid rgba(143, 112, 108, 0.8);
  background: transparent;
  padding: 10px 0;
  color: var(--text);
}

.card-search {
  min-width: 340px;
}

.card-search input {
  min-width: 250px;
}

.history-toolbar label,
.editorial-form label {
  display: grid;
  gap: 10px;
}

.history-toolbar span,
.editorial-form span {
  font-size: 0.72rem;
  color: var(--muted);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.history-toolbar input,
.history-toolbar select,
.editorial-form input,
.editorial-form select,
.editorial-form textarea {
  width: 100%;
  border: none;
  border-bottom: 2px solid rgba(143, 112, 108, 0.8);
  background: transparent;
  padding: 10px 0;
  color: var(--text);
}

.editorial-form textarea {
  resize: vertical;
  min-height: 92px;
}

.entry-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 28px;
}

.transaction-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  background: rgba(26, 28, 28, 0.18);
  backdrop-filter: blur(12px);
  display: grid;
  place-items: center;
  padding: 32px;
}

.transaction-modal {
  width: min(920px, 100%);
  max-height: calc(100vh - 64px);
  overflow: auto;
  background: rgba(255, 255, 255, 0.96);
  padding: 36px;
  border-radius: 0.75rem;
  box-shadow: 0 32px 80px rgba(163, 1, 19, 0.12);
}

.asset-detail-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
  background: rgba(26, 28, 28, 0.24);
  backdrop-filter: blur(12px);
  padding: 28px;
  overflow: auto;
}

.asset-detail-modal {
  width: min(1320px, 100%);
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.97);
  border-radius: 0.85rem;
  box-shadow: 0 40px 100px rgba(163, 1, 19, 0.14);
}

.asset-detail-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(320px, 0.75fr);
  min-height: calc(100vh - 56px);
}

.asset-detail-main {
  padding: 34px;
  border-right: 1px solid rgba(228, 190, 186, 0.35);
  display: grid;
  gap: 24px;
  align-content: start;
}

.asset-detail-head,
.asset-detail-hero,
.detail-chart-meta,
.detail-transaction-row,
.detail-metric {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.asset-detail-head {
  align-items: center;
}

.detail-back {
  border: none;
  background: var(--surface-low);
  color: var(--text);
  padding: 12px 16px;
  border-radius: 999px;
  font-weight: 700;
  cursor: pointer;
}

.detail-ref {
  color: var(--muted);
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.16em;
}

.asset-detail-hero {
  align-items: end;
}

.asset-detail-hero h2,
.detail-side-card h3,
.detail-chart-card h3,
.detail-ledger-card h3 {
  margin: 0;
  font-family: 'Manrope', sans-serif;
  letter-spacing: -0.04em;
}

.asset-detail-hero h2 {
  font-size: 3rem;
}

.asset-detail-hero p,
.detail-side-card p,
.detail-empty-state,
.detail-summary-copy {
  margin: 8px 0 0;
  color: var(--muted);
  line-height: 1.6;
}

.asset-detail-price {
  text-align: right;
}

.asset-detail-price strong {
  display: block;
  font-family: 'Manrope', sans-serif;
  font-size: 2.6rem;
  letter-spacing: -0.05em;
}

.detail-chart-card,
.detail-ledger-card,
.detail-side-card {
  background: #fcfcfc;
  border-radius: 0.75rem;
  padding: 26px;
}

.detail-spark-wrap {
  padding-top: 12px;
}

.detail-sparkline {
  width: 100%;
  height: 220px;
  display: block;
}

.detail-performance-chart {
  height: 260px;
}

.detail-sparkline-path {
  stroke-width: 4;
}

.detail-chart-meta {
  margin-top: 14px;
  align-items: center;
  color: var(--muted);
  font-size: 0.82rem;
}

.detail-transaction-list,
.detail-metric-list {
  display: grid;
  gap: 14px;
}

.detail-transaction-row,
.detail-metric {
  align-items: start;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(228, 190, 186, 0.28);
}

.detail-transaction-row strong,
.detail-metric strong {
  display: block;
  font-family: 'Manrope', sans-serif;
}

.detail-transaction-row span,
.detail-metric span {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-size: 0.82rem;
}

.detail-transaction-numbers {
  text-align: right;
}

.asset-detail-side {
  padding: 34px;
  display: grid;
  gap: 20px;
  align-content: start;
  background: linear-gradient(180deg, #faf7f6 0%, #f3efed 100%);
}

.detail-side-card-primary {
  background: linear-gradient(180deg, #ffffff 0%, #f7efec 100%);
}

.detail-action-stack {
  margin-top: 18px;
  display: grid;
  gap: 12px;
}

.detail-action-stack .sidebar-primary,
.detail-action-stack .sidebar-secondary {
  width: 100%;
}

.detail-empty-state {
  padding: 20px 0 4px;
}

.transaction-modal-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  margin-bottom: 28px;
}

.transaction-modal-actions {
  display: flex;
  gap: 18px;
  align-items: flex-start;
}

.modal-close {
  border: none;
  background: var(--surface-low);
  color: var(--muted);
  padding: 12px 16px;
  border-radius: 999px;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  cursor: pointer;
}

.entry-heading h2 {
  margin: 0;
  font-size: 2.6rem;
  letter-spacing: -0.05em;
}

.editorial-form {
  display: grid;
  gap: 28px;
}

.form-section {
  display: grid;
  gap: 18px;
}

.form-section-header {
  border-left: 4px solid var(--primary);
  padding-left: 14px;
}

.form-section-header h3 {
  margin: 0;
  color: var(--primary);
  font-size: 1.18rem;
  text-transform: uppercase;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.form-span-2 {
  grid-column: span 2;
}

.entry-preview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 18px;
  background: var(--surface-low);
}

.entry-preview span {
  display: block;
  color: var(--muted);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.14em;
}

.entry-preview strong {
  display: block;
  margin-top: 8px;
  font-family: 'Manrope', sans-serif;
  font-size: 1.3rem;
  color: var(--gain);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.text-rise,
.text-holding-rise {
  color: var(--gain);
}

.text-fall,
.text-holding-fall {
  color: var(--loss);
}

.trend-cell {
  min-width: 116px;
}

.trend-hover-card {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 10px;
  background: var(--surface-low);
  border-radius: 999px;
  cursor: pointer;
}

.sparkline {
  width: 96px;
  height: 28px;
  display: block;
}

.sparkline-large {
  width: 180px;
  height: 72px;
}

.sparkline-path {
  fill: none;
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sparkline-path.text-holding-rise,
.sparkline-path.text-rise {
  stroke: var(--gain);
}

.sparkline-path.text-holding-fall,
.sparkline-path.text-fall {
  stroke: var(--loss);
}

.trend-empty {
  color: var(--muted);
}

.trend-floating-popover {
  position: fixed;
  z-index: 60;
  width: 220px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(24px);
  box-shadow: 0 24px 64px rgba(163, 1, 19, 0.12);
}

.trend-popover-header,
.trend-popover-meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.trend-popover-header {
  margin-bottom: 10px;
}

.trend-popover-header span,
.trend-popover-meta span {
  font-size: 0.74rem;
  color: var(--muted);
}

.trend-popover-header strong {
  font-family: 'Manrope', sans-serif;
}

.trend-popover-meta {
  margin-top: 10px;
}

@media (max-width: 1180px) {
  .ledger-shell {
    grid-template-columns: 1fr;
  }

  .editorial-sidebar {
    position: relative;
    height: auto;
  }

  .bento-grid,
  .activity-row-section,
  .insight-grid {
    grid-template-columns: 1fr;
  }

  .asset-detail-shell {
    grid-template-columns: 1fr;
  }

  .asset-detail-main {
    border-right: none;
    border-bottom: 1px solid rgba(228, 190, 186, 0.35);
  }
}

@media (max-width: 768px) {
  .editorial-topbar,
  .editorial-canvas,
  .editorial-sidebar {
    padding: 20px;
  }

  .topbar-brand,
  .hero-ledger,
  .entry-heading,
  .transaction-modal-head,
  .transaction-modal-actions,
  .form-actions,
  .asset-detail-head,
  .asset-detail-hero,
  .detail-chart-meta,
  .detail-transaction-row,
  .detail-metric,
  .card-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .card-search {
    width: 100%;
  }

  .card-search input {
    min-width: 0;
    width: 100%;
  }

  .hero-ledger h1 {
    font-size: 2.5rem;
  }

  .hero-value {
    font-size: 2.8rem;
  }

  .history-toolbar,
  .form-grid,
  .entry-preview,
  .editorial-summary {
    grid-template-columns: 1fr;
  }

  .research-controls {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .activity-grid {
    grid-template-columns: 1fr 1fr;
  }

  .mini-metric-wide {
    grid-column: auto;
  }

  .form-span-2 {
    grid-column: span 1;
  }

  .asset-detail-backdrop {
    padding: 12px;
  }

  .asset-detail-main,
  .asset-detail-side,
  .transaction-modal {
    padding: 22px;
  }
}
</style>
