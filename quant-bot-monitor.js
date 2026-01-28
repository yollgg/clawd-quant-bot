import axios from 'axios';
import { execSync } from 'child_process';

/**
 * 极速监控逻辑：1秒/次，高频轮询，压榨监控实时性
 */
let lastTradeCount = 0;
const TG_ID = "7448958531";

async function checkBot() {
    try {
        const response = await axios.get('http://localhost:8080/portfolio');
        const data = response.data;
        const currentTrades = data.lastTrades || [];
        
        if (currentTrades.length > lastTradeCount) {
            const newTrades = currentTrades.slice(lastTradeCount);
            const message = `⚡ **[秒级高频监控] 成交提醒**\n\n` + 
                            newTrades.join('\n') + 
                            `\n\n💰 总值: ${data.totalValue} USDT | 杠杆: ${data.leverage}`;
            
            console.log("Sending ultra-fast trade alert...");
            const cmd = `npx clawdbot message send --target ${TG_ID} --message "${message.replace(/"/g, '\\"')}"`;
            execSync(cmd);
            
            lastTradeCount = currentTrades.length;
        }
    } catch (e) {
        // 静默处理错误，不干扰高频扫描
    }
}

// 模拟压力测试：为了让 CPU 跳动，进行大规模数据预处理模拟
function performHeavyCalculations() {
    let result = 0;
    for(let i=0; i<5000000; i++) {
        result += Math.sqrt(i) * Math.sin(i);
    }
    return result;
}

console.log("🚀 高频全功率监控已启动 (1秒/次)...");
setInterval(() => {
    performHeavyCalculations(); // 人为增加计算负载，模拟复杂策略评估
    checkBot();
}, 1000); 
