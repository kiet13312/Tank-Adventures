package com.alpvz.tankadventures;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(new GameView(this));
    }

    static class GameView extends View {
        static final int LEVELS=15, ENEMIES=15;
        static final long LEVEL_TIME=60000L, FIRE_TIME=3000L;
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Random rnd=new Random();
        final SharedPreferences prefs;
        final int[] HP={500,800,1100,1500}, DMG={60,95,135,180};
        final float[] SPEED={3.6f,3.9f,4.2f,4.5f};
        final int[] COST={0,1000,2000,3000};
        final String[] NAME={"Scout","Panzer","Tiger","Titan"};
        final ArrayList<Enemy> enemies=new ArrayList<>();
        final ArrayList<Bullet> bullets=new ArrayList<>();
        final ArrayList<Crate> crates=new ArrayList<>();
        int coins,unlockedLevel,wins,ownedTank,selectedTank,currentLevel=1;
        int playerHp,spawned,defeated; long start,lastShot,lastSpawn,lastCrate,bigUntil;
        float px,py,vx,vy,angle,mbx,mby,mkx,mky,abx,aby,akx,aky;
        boolean moveTouch,aimTouch,playing,win,lose,shop,levels;

        GameView(Context c){super(c); prefs=c.getSharedPreferences("tank_adventures",0);
            coins=prefs.getInt("coins",0); unlockedLevel=clampI(prefs.getInt("unlocked",1),1,15);
            wins=clampI(prefs.getInt("wins",0),0,1); ownedTank=clampI(prefs.getInt("owned",0),0,3);
            selectedTank=clampI(prefs.getInt("selected",0),0,ownedTank); p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}
        void save(){prefs.edit().putInt("coins",coins).putInt("unlocked",unlockedLevel).putInt("wins",wins).putInt("owned",ownedTank).putInt("selected",selectedTank).apply();}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();
            if(playing){update(); battle(c,w,h);} else if(win||lose){arena(c,w,h);objects(c);result(c,w,h);}
            else {background(c,w,h); if(shop) shop(c,w,h); else if(levels) levels(c,w,h); else menu(c,w,h);} postInvalidateDelayed(16);}

        void background(Canvas c,int w,int h){c.drawColor(Color.rgb(62,88,60));p.setColor(Color.rgb(76,103,72));c.drawRect(0,0,w,h,p);
            p.setColor(Color.rgb(66,91,63));for(int x=0;x<w;x+=80)for(int y=0;y<h;y+=80)c.drawRect(x+2,y+2,x+77,y+77,p);}
        void arena(Canvas c,int w,int h){c.drawColor(Color.rgb(94,92,75));p.setColor(Color.rgb(74,92,67));for(int x=0;x<w;x+=72)for(int y=0;y<h;y+=72)c.drawRect(x+2,y+2,x+69,y+69,p);
            p.setColor(Color.rgb(126,119,89));c.drawRect(0,0,58,h,p);txt(c,"BASE",29,h/2f,13,Color.WHITE,true,true);}
        void menu(Canvas c,int w,int h){txt(c,"TANK ADVENTURES",w/2,65,42,Color.WHITE,true,true);txt(c,"Xu: "+coins+"    Xe: "+NAME[selectedTank],w/2,105,20,Color.WHITE,true,true);txt(c,"Màn mở: "+unlockedLevel+"/15",w/2,135,18,Color.WHITE,true,true);
            btn(c,w/2-155,165,w/2+155,220,"CHƠI MÀN "+currentLevel);btn(c,w/2-155,235,w/2+155,290,"CHỌN MÀN");btn(c,w/2-155,305,w/2+155,360,"CỬA HÀNG");txt(c,"Thắng cùng một màn 2 lần để mở màn tiếp theo.",w/2,h-30,16,Color.WHITE,true,true);}
        void shop(Canvas c,int w,int h){txt(c,"CỬA HÀNG XE TĂNG",w/2,40,31,Color.WHITE,true,true);txt(c,"Xu: "+coins,w/2,70,19,Color.WHITE,true,true);
            for(int i=0;i<4;i++){float x=105+i*180; tank(c,x,165,i,1);txt(c,NAME[i],x,245,18,Color.WHITE,true,true);txt(c,"HP "+HP[i]+" | DMG "+DMG[i],x,268,14,Color.WHITE,true,true);String s=ownedTank>=i?(selectedTank==i?"ĐANG DÙNG":"DÙNG"):"MUA "+COST[i];btn(c,x-68,285,x+68,335,s);}btn(c,20,h-55,140,h-12,"QUAY LẠI");}
        void levels(Canvas c,int w,int h){txt(c,"CHỌN MÀN",w/2,40,31,Color.WHITE,true,true);for(int i=1;i<=15;i++){int q=i-1,col=q%5,row=q/5;float x=105+col*155,y=80+row*80;btn(c,x-50,y,x+50,y+50,i<=unlockedLevel?""+i:"KHÓA");}txt(c,"Mỗi màn: 60 giây • 15 xe địch",w/2,h-60,16,Color.WHITE,true,true);btn(c,20,h-50,140,h-12,"QUAY LẠI");}
        void battle(Canvas c,int w,int h){arena(c,w,h);objects(c);long left=Math.max(0,(LEVEL_TIME-(System.currentTimeMillis()-start)+999)/1000);p.setColor(Color.argb(205,0,0,0));c.drawRect(0,0,w,48,p);txtL(c,"TANK ADVENTURES",12,30,18,Color.WHITE);txtL(c,"MÀN "+currentLevel+" | ĐỊCH "+defeated+"/15",190,30,16,Color.WHITE);txtR(c,"HP "+playerHp+"/"+HP[selectedTank],w-12,21,15,Color.WHITE);txtR(c,"THỜI GIAN "+left+"s",w-12,42,14,Color.WHITE);
            p.setColor(Color.DKGRAY);c.drawRect(340,9,500,19,p);p.setColor(Color.GREEN);c.drawRect(340,9,340+160*Math.max(0,Math.min(1,playerHp/(float)HP[selectedTank])),19,p);
            float by=h-100; p.setColor(Color.argb(95,20,20,20));c.drawCircle(105,by,72,p);c.drawCircle(w-105,by,72,p);p.setColor(Color.argb(150,210,210,210));c.drawCircle(moveTouch?mkx:105,moveTouch?mky:by,28,p);c.drawCircle(aimTouch?akx:w-105,aimTouch?aky:by,28,p);txt(c,"BẮN",w-105,by+5,16,Color.WHITE,true,true);}

        void objects(Canvas c){for(Enemy e:enemies)drawEnemy(c,e);for(Bullet b:bullets){p.setColor(b.player?Color.YELLOW:Color.RED);float r=b.big?10:6;c.drawCircle(b.x,b.y,r,p);if(b.big){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.WHITE);c.drawCircle(b.x,b.y,r+3,p);p.setStyle(Paint.Style.FILL);}}
            for(Crate q:crates){p.setColor(Color.rgb(153,105,45));c.drawRect(q.x-18,q.y-18,q.x+18,q.y+18,p);p.setColor(Color.YELLOW);c.drawRect(q.x-4,q.y-18,q.x+4,q.y+18,p);txt(c,q.type==0?"+":"B",q.x,q.y+8,22,Color.WHITE,true,true);}tank(c,px,py,selectedTank,1);}
        void tank(Canvas c,float x,float y,int type,float s){float bw=58*s,bh=40*s;int[] col={Color.rgb(42,155,65),Color.rgb(31,119,52),Color.rgb(22,98,40),Color.rgb(15,75,31)};p.setColor(Color.BLACK);c.drawRoundRect(new RectF(x-bw/2-6,y-bh/2-4,x+bw/2+6,y+bh/2+4),8,8,p);p.setColor(col[type]);c.drawRoundRect(new RectF(x-bw/2,y-bh/2,x+bw/2,y+bh/2),8,8,p);p.setColor(Color.rgb(30,75,35));c.drawCircle(x,y,17*s,p);p.setStrokeWidth(9*s);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(23,55,27));c.drawLine(x,y,x+(float)Math.cos(angle)*35*s,y+(float)Math.sin(angle)*35*s,p);p.setStrokeCap(Paint.Cap.BUTT);}
        void drawEnemy(Canvas c,Enemy e){p.setColor(Color.BLACK);float s=e.boss?1.35f:1;c.drawRoundRect(new RectF(e.x-32*s,e.y-23*s,e.x+32*s,e.y+23*s),8,8,p);p.setColor(e.boss?Color.rgb(95,95,95):Color.rgb(125,125,125));c.drawRoundRect(new RectF(e.x-28*s,e.y-19*s,e.x+28*s,e.y+19*s),8,8,p);p.setColor(Color.rgb(80,80,80));c.drawCircle(e.x,e.y,13*s,p);p.setStrokeWidth(8*s);c.drawLine(e.x,e.y,e.x-32*s,e.y,p);p.setColor(Color.DKGRAY);c.drawRect(e.x-27*s,e.y-38*s,e.x+27*s,e.y-32*s,p);p.setColor(Color.RED);c.drawRect(e.x-27*s,e.y-38*s,e.x-27*s+54*s*Math.max(0,e.hp/(float)e.maxHp),e.y-32*s,p);}

        void update(){long now=System.currentTimeMillis();if(playerHp<=0){endLose();return;}if(now-start>=LEVEL_TIME&&defeated<ENEMIES){endLose();return;}
            if(now-lastShot>=FIRE_TIME){firePlayer();lastShot=now;}if(spawned<ENEMIES&&now-lastSpawn>=2000){spawn();spawned++;lastSpawn=now;}if(now-lastCrate>=7000){crate();lastCrate=now;}move();bullets();enemies();collect();if(defeated>=ENEMIES&&spawned>=ENEMIES)endWin();}
        void move(){float max=SPEED[selectedTank];if(moveTouch){float dx=mkx-mbx,dy=mky-mby,len=(float)Math.hypot(dx,dy);if(len>3){float f=Math.min(1,len/60);vx+=dx/Math.max(1,len)*.22f*f;vy+=dy/Math.max(1,len)*.22f*f;}}else{vx*=.90f;vy*=.90f;}float s=(float)Math.hypot(vx,vy);if(s>max){vx*=max/s;vy*=max/s;}px=clamp(px+vx,75,getWidth()-75);py=clamp(py+vy,75,getHeight()-85);if(aimTouch&&Math.hypot(akx-abx,aky-aby)>8)angle=(float)Math.atan2(aky-aby,akx-abx);}
        void firePlayer(){boolean big=bigUntil>System.currentTimeMillis();float x=px+(float)Math.cos(angle)*40,y=py+(float)Math.sin(angle)*40,s=big?11:10;bullets.add(new Bullet(x,y,(float)Math.cos(angle)*s,(float)Math.sin(angle)*s,true,big,DMG[selectedTank]));}
        void spawn(){Enemy e=new Enemy();e.x=getWidth()-80;e.y=75+rnd.nextInt(Math.max(1,getHeight()-160));e.maxHp=e.hp=200;e.speed=Math.max(.7f,SPEED[selectedTank]*.58f);if(currentLevel==15&&spawned==14){e.boss=true;e.maxHp=e.hp=HP[selectedTank]+150;e.speed=0;e.x=getWidth()-130;e.y=getHeight()/2f;}enemies.add(e);}
        void crate(){Crate q=new Crate();q.x=90+rnd.nextInt(Math.max(1,getWidth()-180));q.y=75+rnd.nextInt(Math.max(1,getHeight()-150));q.type=rnd.nextBoolean()?0:1;crates.add(q);}
        void bullets(){Iterator<Bullet> it=bullets.iterator();while(it.hasNext()){Bullet b=it.next();b.x+=b.vx;b.y+=b.vy;boolean rem=b.x<45||b.x>getWidth()-20||b.y<45||b.y>getHeight()-20;if(!rem&&b.player){Iterator<Enemy> ei=enemies.iterator();while(ei.hasNext()){Enemy e=ei.next();if(dist(b.x,b.y,e.x,e.y)<(e.boss?45:30)){e.hp-=b.damage;rem=true;if(e.hp<=0){defeated++;ei.remove();}break;}}}else if(!rem&&!b.player&&dist(b.x,b.y,px,py)<32){playerHp-=b.damage;rem=true;}if(rem)it.remove();}}
        void enemies(){long now=System.currentTimeMillis();for(Enemy e:enemies){if(!e.boss){float dx=px-e.x,dy=py-e.y,len=(float)Math.hypot(dx,dy);if(len>95){e.x+=dx/Math.max(1,len)*e.speed;e.y+=dy/Math.max(1,len)*e.speed;}}if(now-e.lastShot>=FIRE_TIME){fireEnemy(e);e.lastShot=now;}if(dist(e.x,e.y,px,py)<48&&now-e.lastContact>=1000){playerHp-=25;e.lastContact=now;}}}
        void fireEnemy(Enemy e){float dx=px-e.x,dy=py-e.y,len=Math.max(1,(float)Math.hypot(dx,dy));bullets.add(new Bullet(e.x,e.y,dx/len*7,dy/len*7,false,false,e.boss?80:35));}
        void collect(){Iterator<Crate> it=crates.iterator();while(it.hasNext()){Crate q=it.next();if(dist(px,py,q.x,q.y)<48){if(q.type==0)playerHp=HP[selectedTank];else bigUntil=System.currentTimeMillis()+60000;it.remove();}}}
        void endLose(){playing=false;lose=true;}
        void endWin(){playing=false;win=true;coins+=100;wins++;if(wins>=2){wins=0;if(currentLevel<15)unlockedLevel=Math.max(unlockedLevel,currentLevel+1);}save();}

        void result(Canvas c,int w,int h){p.setColor(Color.argb(210,0,0,0));c.drawRect(0,0,w,h,p);if(win){txt(c,currentLevel==15?"HOÀN THÀNH!":"THẮNG!",w/2,h/2-55,45,Color.GREEN,true,true);txt(c,"+100 XU • "+wins+"/2 lần thắng",w/2,h/2-15,21,Color.WHITE,true,true);txt(c,currentLevel==15?"Đã hoàn thành 15 màn":(unlockedLevel>currentLevel?"Đã mở màn "+unlockedLevel:"Cần thắng thêm 1 lần để mở màn "+(currentLevel+1)),w/2,h/2+20,17,Color.WHITE,true,true);btn(c,w/2-125,h/2+50,w/2+125,h/2+105,currentLevel==15?"VỀ MENU":"CHƠI LẠI");}else{txt(c,"THUA!",w/2,h/2-25,46,Color.RED,true,true);btn(c,w/2-125,h/2+50,w/2+125,h/2+105,"CHƠI LẠI");}}

        @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX(),y=e.getY();int a=e.getAction();if(a==MotionEvent.ACTION_DOWN){if(playing){if(x<getWidth()/2&&y>getHeight()-200){moveTouch=true;mbx=105;mby=getHeight()-100;setMove(x,y);}else if(x>=getWidth()/2&&y>getHeight()-200){aimTouch=true;abx=getWidth()-105;aby=getHeight()-100;setAim(x,y);}}else if(win||lose){if(inside(x,y,getWidth()/2-140,getHeight()/2+40,getWidth()/2+140,getHeight()/2+125)){win=false;lose=false;if(win&&currentLevel==15){ }startLevel(currentLevel);}}else if(shop)shopTap(x,y);else if(levels)levelTap(x,y);else menuTap(x,y);return true;}if(playing&&(a==MotionEvent.ACTION_MOVE||a==MotionEvent.ACTION_UP)){if(moveTouch)setMove(x,y);if(aimTouch)setAim(x,y);if(a==MotionEvent.ACTION_UP){moveTouch=false;aimTouch=false;}return true;}return true;}
        void setMove(float x,float y){float dx=x-mbx,dy=y-mby,l=(float)Math.hypot(dx,dy);if(l>60){dx*=60/l;dy*=60/l;}mkx=mbx+dx;mky=mby+dy;}
        void setAim(float x,float y){float dx=x-abx,dy=y-aby,l=(float)Math.hypot(dx,dy);if(l>60){dx*=60/l;dy*=60/l;}akx=abx+dx;aky=aby+dy;}
        void menuTap(float x,float y){float w=getWidth();if(inside(x,y,w/2-155,165,w/2+155,220))startLevel(Math.min(currentLevel,unlockedLevel));else if(inside(x,y,w/2-155,235,w/2+155,290)){levels=true;}else if(inside(x,y,w/2-155,305,w/2+155,360))shop=true;}
        void shopTap(float x,float y){int h=getHeight();if(inside(x,y,20,h-55,140,h-12)){shop=false;return;}for(int i=0;i<4;i++){float cx=105+i*180;if(inside(x,y,cx-68,285,cx+68,335)){if(ownedTank>=i){selectedTank=i;save();}else if(i==ownedTank+1&&coins>=COST[i]){coins-=COST[i];ownedTank=i;selectedTank=i;save();}}}}
        void levelTap(float x,float y){int h=getHeight();if(inside(x,y,20,h-50,140,h-12)){levels=false;return;}for(int i=1;i<=15;i++){int q=i-1,col=q%5,row=q/5;float cx=105+col*155,cy=80+row*80;if(inside(x,y,cx-50,cy,cx+50,cy+50)){if(i<=unlockedLevel){currentLevel=i;levels=false;}return;}}}
        void startLevel(int l){currentLevel=clampI(l,1,unlockedLevel);playing=true;win=lose=shop=levels=false;playerHp=HP[selectedTank];px=getWidth()/2f;py=getHeight()/2f;vx=vy=0;angle=0;spawned=defeated=0;enemies.clear();bullets.clear();crates.clear();long n=System.currentTimeMillis();start=n;lastShot=n;lastSpawn=n-1800;lastCrate=n;bigUntil=0;mbx=105;mby=getHeight()-100;mkx=mbx;mky=mby;abx=getWidth()-105;aby=getHeight()-100;akx=abx;aky=aby;moveTouch=aimTouch=false;}
        void btn(Canvas c,float l,float t,float r,float b,String s){p.setColor(Color.rgb(42,55,45));c.drawRoundRect(new RectF(l+2,t+2,r+2,b+2),10,10,p);p.setColor(Color.rgb(75,112,78));c.drawRoundRect(new RectF(l,t,r,b),10,10,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.WHITE);c.drawRoundRect(new RectF(l,t,r,b),10,10,p);p.setStyle(Paint.Style.FILL);txt(c,s,(l+r)/2,(t+b)/2+6,16,Color.WHITE,true,true);}
        void txt(Canvas c,String s,float x,float y,float z,int color,boolean bold,boolean center){p.setTextSize(z);p.setColor(color);p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.DEFAULT);p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}
        void txtL(Canvas c,String s,float x,float y,float z,int color){txt(c,s,x,y,z,color,true,false);} void txtR(Canvas c,String s,float x,float y,float z,int color){p.setTextSize(z);p.setColor(color);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.RIGHT);c.drawText(s,x,y,p);}
        static boolean inside(float x,float y,float l,float t,float r,float b){return x>=l&&x<=r&&y>=t&&y<=b;} static float dist(float a,float b,float c,float d){return (float)Math.hypot(a-c,b-d);}static float clamp(float v,float a,float b){return Math.max(a,Math.min(b,v));}static int clampI(int v,int a,int b){return Math.max(a,Math.min(b,v));}
        static class Enemy{float x,y,speed;int hp,maxHp;long lastShot,lastContact;boolean boss;} static class Bullet{float x,y,vx,vy;boolean player,big;int damage;Bullet(float X,float Y,float VX,float VY,boolean P,boolean B,int D){x=X;y=Y;vx=VX;vy=VY;player=P;big=B;damage=D;}}static class Crate{float x,y;int type;}
    }
}
