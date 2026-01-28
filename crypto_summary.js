import axios from 'axios';

/**
 * Basic Crypto Price Scraper
 * Gets prices from CoinGecko API (public)
 */
async function getCryptoPrices() {
  try {
    const response = await axios.get('https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana,binancecoin,ripple&vs_currencies=usd&include_24hr_change=true');
    const data = response.data;
    
    console.log('--- Crypto Price Summary ---');
    for (const [id, info] of Object.entries(data)) {
      const price = info.usd;
      const change = info.usd_24h_change.toFixed(2);
      const symbol = change >= 0 ? '📈' : '📉';
      console.log(`${id.toUpperCase()}: $${price} (${symbol} ${change}%)`);
    }

    console.log('\n--- Spot Portfolio Suggestion (General) ---');
    console.log('1. Blue Chips (BTC/ETH): 60% - Core stability.');
    console.log('2. Mid-Caps (SOL/BNB): 20% - Growth potential.');
    console.log('3. Speculative/New Alts: 10% - High risk/reward.');
    console.log('4. Cash/Stables: 10% - For buying dips.');
  } catch (error) {
    console.error('Error fetching crypto prices:', error.message);
  }
}

getCryptoPrices();
