// Set NODE_PATH to an installation containing mineflayer and minecraft-data.
const data = require('minecraft-data');
const mineflayer = require('mineflayer');
data('26.1').version.version = 776;
data.versionsByMinecraftVersion.pc['26.1'].version = 776;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
async function run(name) {
  const bot = mineflayer.createBot({host:'127.0.0.1', port:25871, username:name, auth:'offline', version:'26.1'});
  bot.on('error', e => console.error(name, e.message));
  bot.on('kicked', e => console.error(name, 'KICK', e));
  await new Promise(resolve => bot.once('spawn', resolve));
  bot._client.write('player_loaded', {});
  const tick = setInterval(() => bot._client.write('tick_end', {}), 50);
  try {
    await delay(2500);
    const commands = process.env.BREWERY_SCENARIO ? process.env.BREWERY_SCENARIO.split(',') : ['enable','holder','identities','direct','one-tick','metadata','puke','drunk'];
    for (const command of commands) {
      // Config is global; do not overlap temporary configuration changes.
      if (command === 'one-tick' && name.endsWith('2')) continue;
      bot.chat('/brewprobe ' + command);
      await delay(700);
    }
    if (!process.env.BREWERY_SCENARIO) {
      bot.chat('/breweryx puke ' + (name.endsWith('2') ? 'BrewTest1' : 'BrewTest2') + ' 4');
      await delay(14000);
    }
    bot.chat('/brewprobe summary');
    await delay(500);
  } finally {clearInterval(tick); bot.quit();}
}
Promise.all(process.env.BREWERY_SCENARIO ? [run('BrewTest1')] : [run('BrewTest1'),run('BrewTest2')]).then(() => process.exit(0)).catch(e => {console.error(e);process.exit(1)});
setTimeout(() => process.exit(2), 45000).unref();
