# Crypto Monitor Skill

Use this skill to monitor the cryptocurrency market and generate summaries.

## Core Capabilities
- Fetch latest news from major crypto news outlets.
- Monitor price movements for top cryptocurrencies (BTC, ETH, etc.).
- Generate concise morning digests.
- Provide spot portfolio strategy advice based on current market trends.

## Tools to Use
- `web_search`: Search for "latest crypto news", "BTC price today", etc.
- `web_fetch`: Extract content from top crypto sites (Coindesk, Cointelegraph, etc.).

## Workflow
1. Run a `web_search` for major market news in the last 24 hours.
2. Run a `web_search` for current prices of BTC, ETH, and other top 10 coins.
3. Synthesize the information into a structured summary:
    - **Market Sentiment**: Overall mood.
    - **Price Watch**: Key movements.
    - **Top Headlines**: 3-5 major stories.
    - **Spot Portfolio Strategy**: 
        - Balanced approach (e.g., 50% BTC/ETH, 30% Altcoins, 20% Stablecoins).
        - Suggested entries/exits based on technical levels.
    - **Actionable Insight**: Any significant trends to watch.
