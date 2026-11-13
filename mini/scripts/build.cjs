const fs=require('node:fs');const path=require('node:path');
for(const name of ['app.json','app.wxss']) fs.copyFileSync(name,path.join('dist',name));
for(const page of ['login','home','records']) for(const ext of ['json','wxml','wxss']) fs.copyFileSync(`pages/${page}/${page}.${ext}`,`dist/pages/${page}/${page}.${ext}`);
