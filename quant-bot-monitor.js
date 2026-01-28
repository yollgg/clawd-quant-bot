import axios from 'axios';
import { execSync } from 'child_process';

let lastTradeCount = 0;
const TG_ID = "7448958531";

async function checkBot() {
    try {
        const response = await axios.get('http://localhost:8080/portfolio');
        const data = response.data;
        const currentTrades = data.lastTrades || [];
        
        if (currentTrades.length > lastTradeCount) {
            const newTrades = currentTrades.slice(lastTradeCount);
            const message = `🤖 **量化成交预警**\n\n` + 
                            newTrades.join('\n') + 
                            `\n\n💰 当前总价值: ${data.totalValueUsdt} USDT\n持仓: ${data.balanceBtc} BTC`;
            
            console.log("Sending trade alert...");
            // Correct option is --target or -t
            const cmd = `npx clawdbot message send --target ${TG_ID} --message "${message.replace(/"/g, '\\"')}"`;
            execSync(cmd);
            
            lastTradeCount = currentTrades.length;
        }
    } catch (e) {
        console.error("Monitor error:", e.message);
    }
}

console.log("Automation Monitor Started...");
setInterval(checkBot, 10000); // Check every 10 seconds
