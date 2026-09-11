package com.alpvz.tankadventures;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);setContentView(new GameView(this));}

    static class GameView extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Path path=new Path(); final Random r=new Random(); final SharedPreferences sp;
        final int[] HP={500,800,1100,1500}, DMG={50,75,100,130}, COST={0,1000,2000,3000}; final float[] SPEED={4.4f,4.8f,5.2f,5.7f};
        final ArrayList<Enemy> es=new ArrayList<>(); final ArrayList<Shot> ss=new ArrayList<>();
        int coins,owned=0,selected=0,level=1,wins=0,hp,ammo,kills,engine,tracks; float x,y,vx,vy,cam,tilt; boolean playing,win,lose,fire,shop,levels; long start,lastReload,lastEnemy;
        float jx=105,jy=0,kx=105,ky=0;
        GameView(Context c){super(c);sp=c.getSharedPreferences("tank_adventures",0);coins=sp.getInt("coins",0);owned=sp.getInt("owned",0);selected=sp.getInt("selected",0);level=sp.getInt("level",1);wins=sp.getInt("wins",0);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}
        void save(){sp.edit().putInt("coins",coins).putInt("owned",owned).putInt("selected",selected).putInt("level",level).putInt("wins",wins).apply();}
        @Override protected void onDraw(Canvas c){if(playing){update();draw(c);}else{menu(c);}postInvalidateDelayed(16);}
        void start(){playing=true;win=lose=false;shop=levels=false;hp=HP[selected];ammo=5;engine=100;tracks=100;kills=0;x=120;y=0;vx=0;vy=0;cam=0;tilt=0;es.clear();ss.clear();r.setSeed(level*997+selected);for(int i=0;i<10;i++){Enemy e=new Enemy();e.x=800+i*380+r.nextInt(180);e.hp=200+level*12+(i%3)*80;e.max=e.hp;e.speed=1.0f+level*.025f;e.boss=false;es.add(e);}Enemy b=new Enemy();b.x=5200+level*180;b.hp=700+level*45;b.max=b.hp;b.speed=.55f;b.boss=true;es.add(b);start=System.currentTimeMillis();lastReload=start;lastEnemy=start;}
        float ground(float wx){return getHeight()*.69f+(float)Math.sin(wx*.0047)*65+(float)Math.sin(wx*.011+level)*24;}
        void update(){long now=System.currentTimeMillis();float dt=Math.min(2.2f,Math.max(.5f,(now-start)%10000/16.666f+0.5f));if(hp<=0){lose=true;playing=false;return;}if(now-start>60000){win=true;playing=false;wins++;coins+=100;if(wins>=2){wins=0;if(level<15)level++;}save();return;}
            float dir=(kx-jx)/55f;if(Math.abs(dir)<.1f)dir=0;vx+=dir*.11f*dt;if(!fire&&!shop&&!levels)vx*=.985f;float max=SPEED[selected]*(tracks<40?.7f:tracks<=0?.5f:1);if(vx>max)vx=max;if(vx<-max*.45f)vx=-max*.45f;x+=vx*dt*1.7f;
            float gy=ground(x),ty=gy-48;vy+=.48f*dt;y+=vy*dt;if(y>=ty){float impact=Math.abs(vy);if(impact>7){int d=(int)Math.min(35,impact*2);hp-=Math.max(2,d/3);engine=Math.max(0,engine-d);tracks=Math.max(0,tracks-d/2);}y=ty;vy=-Math.min(4.5f,impact*.2f);}tilt=(float)Math.atan2(ground(x+25)-ground(x-25),50);cam+=(x-getWidth()*.3f-cam)*.11f;if(cam<0)cam=0;
            if(ammo<5&&now-lastReload>=3000){ammo++;lastReload=now;}if(fire&&ammo>0&&now-lastEnemy>=350){shoot(true);ammo--;lastEnemy=now;if(ammo<5)lastReload=now;}
            for(Enemy e:es){if(e.dead)continue;float dx=x-e.x,dy=y-(ground(e.x)-48),d=Math.max(1,(float)Math.hypot(dx,dy));if(!e.boss)e.x+=dx/d*e.speed*dt;else if(Math.abs(dx)>380)e.x+=dx/d*e.speed*dt;if(now-e.last>=3000){enemyShoot(e);e.last=now;}if(d<60&&now-e.contact>900){hp-=e.boss?35:18;tracks=Math.max(0,tracks-(e.boss?12:6));e.contact=now;}}
            Iterator<Shot> it=ss.iterator();while(it.hasNext()){Shot s=it.next();s.x+=s.vx*dt;s.y+=s.vy*dt;boolean rem=s.x<cam-100||s.x>cam+getWidth()+150;if(!rem&&s.player){for(Enemy e:es)if(!e.dead&&dist(s.x,s.y,e.x,ground(e.x)-48)<55){e.hp-=DMG[selected];if(e.hp<=0){e.dead=true;kills++;coins+=e.boss?150:20;}rem=true;break;}}else if(!rem&&!s.player&&dist(s.x,s.y,x,y)<55){hp-=s.dmg;tracks=Math.max(0,tracks-8);engine=Math.max(0,engine-5);rem=true;}if(rem)it.remove();}
        }
        void shoot(boolean player){Shot s=new Shot();s.player=player;s.x=player?x+58:x-58;s.y=player?y-8:ground(x)-48;s.vx=player?9:-7;s.vy=0;s.dmg=player?DMG[selected]:28;ss.add(s);}
        void enemyShoot(Enemy e){Shot s=new Shot();s.player=false;s.x=e.x;s.y=ground(e.x)-48;float dx=x-s.x,dy=y-s.y,d=Math.max(1,(float)Math.hypot(dx,dy));s.vx=dx/d*6;s.vy=dy/d*6;s.dmg=e.boss?45:24;ss.add(s);}
        void draw(Canvas c){c.drawColor(Color.rgb(82,165,220));p.setColor(Color.rgb(125,150,86));path.reset();path.moveTo(0,getHeight());for(int i=0;i<=getWidth();i+=8)path.lineTo(i,ground(cam+i));path.lineTo(getWidth(),getHeight());path.close();c.drawPath(path,p);for(Enemy e:es)if(!e.dead)enemy(c,e);for(Shot s:ss)shot(c,s);tank(c,x-cam,y,selected,1.25f,tilt,true);hud(c);controls(c);}
        void tank(Canvas c,float x,float y,int t,float s,float a,boolean player){c.save();c.rotate((float)Math.toDegrees(a),x,y);float w=82*s,h=48*s;p.setColor(Color.BLACK);c.drawRoundRect(new RectF(x-w/2-5,y-h/2-4,x+w/2+5,y+h/2+7),9,9,p);p.setColor(player?Color.rgb(38,145,62):Color.rgb(105,107,110));c.drawRoundRect(new RectF(x-w/2,y-h/2,x+w/2,y+h/2),8,8,p);p.setColor(Color.DKGRAY);for(int i=0;i<4;i++)c.drawCircle(x-w*.32f+i*w*.21f,y+h*.48f,10*s,p);p.setColor(player?Color.rgb(22,75,29):Color.rgb(55,56,58));c.drawCircle(x,y-6*s,18*s,p);p.setStrokeWidth(9*s);c.drawLine(x,y-6*s,x+(float)Math.cos(a)*52*s,y-6*s+(float)Math.sin(a)*52*s,p);if(engine<40||tracks<40){p.setColor(Color.argb(150,60,60,60));c.drawCircle(x+22*s,y-30*s,8*s,p);}}
        void enemy(Canvas c,Enemy e){float X=e.x-cam,Y=ground(e.x)-48;if(X<-120||X>getWidth()+120)return;tank(c,X,Y,e.boss?2:1,e.boss?1.5f:1,0,false);p.setColor(Color.DKGRAY);c.drawRect(X-50,Y-70,X+50,Y-63,p);p.setColor(Color.RED);c.drawRect(X-50,Y-70,X-50+100*(e.hp/e.max),Y-63,p);if(e.boss)txt(c,"BOSS",X,Y-78,15,Color.WHITE);}
        void shot(Canvas c,Shot s){float X=s.x-cam;if(X<-20||X>getWidth()+20)return;p.setColor(s.player?Color.YELLOW:Color.rgb(255,120,70));c.drawCircle(X,s.y,7,p);p.setColor(Color.argb(130,255,220,150));c.drawCircle(X,s.y,12,p);}
        void hud(Canvas c){p.setColor(Color.argb(200,20,25,28));c.drawRoundRect(new RectF(10,10,getWidth()-10,83),12,12,p);txtL(c,"MÀN "+level+"/15",22,35,16,Color.WHITE);txtL(c,"HP "+Math.max(0,hp)+"/"+HP[selected],115,35,16,Color.WHITE);txtL(c,"ĐẠN "+ammo+"/5",260,35,16,Color.WHITE);txtL(c,"ĐỊCH "+kills,370,35,16,Color.WHITE);txtR(c,"XU "+coins,getWidth()-22,35,16,Color.WHITE);txtL(c,"XÍCH",115,58,11,Color.WHITE);txtL(c,"MÁY",230,58,11,Color.WHITE);p.setColor(Color.DKGRAY);c.drawRect(145,51,215,57,p);p.setColor(Color.YELLOW);c.drawRect(145,51,145+70*tracks/100f,57,p);p.setColor(Color.DKGRAY);c.drawRect(260,51,330,57,p);p.setColor(Color.CYAN);c.drawRect(260,51,260+70*engine/100f,57,p);if(tracks<=0)txtL(c,"XÍCH HỎNG",345,58,11,Color.rgb(255,190,80));if(engine<=0)txtL(c,"MÁY HỎNG",420,58,11,Color.RED);}
        void controls(Canvas c){float y=getHeight()-88;p.setColor(Color.argb(110,0,0,0));c.drawCircle(105,y,66,p);c.drawCircle(getWidth()-92,y,58,p);p.setColor(Color.argb(180,230,230,230));c.drawCircle(kx,ky==0?y:ky,24,p);txt(c,"←  →",105,y+6,18,Color.WHITE);txt(c,"BẮN",getWidth()-92,y+6,16,Color.WHITE);}
        void menu(Canvas c){c.drawColor(Color.rgb(75,155,215));txt(c,win?"THẮNG!":lose?"XE HỎNG":"TANK ADVENTURES",getWidth()/2,70,40,Color.WHITE);if(!playing&&!win&&!lose){txt(c,"SIDE-SCROLLING COMBAT",getWidth()/2,105,18,Color.WHITE);txt(c,"Địch + đạn + HP + xích/máy hỏng",getWidth()/2,133,16,Color.WHITE);btn(c,getWidth()/2-160,165,getWidth()/2+160,220,"CHƠI MÀN "+level);btn(c,getWidth()/2-160,235,getWidth()/2+160,290,"CỬA HÀNG");}else btn(c,getWidth()/2-140,getHeight()/2,getWidth()/2+140,getHeight()/2+55,"CHƠI LẠI");}
        void btn(Canvas c,float l,float t,float rr,float b,String s){p.setColor(Color.argb(210,20,25,28));c.drawRoundRect(new RectF(l,t,rr,b),12,12,p);txt(c,s,(l+rr)/2,(t+b)/2+6,17,Color.WHITE);}
        void txt(Canvas c,String s,float x,float y,float z,int color){p.setColor(color);p.setTextSize(z);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}void txtL(Canvas c,String s,float x,float y,float z,int color){p.setColor(color);p.setTextSize(z);p.setTextAlign(Paint.Align.LEFT);c.drawText(s,x,y,p);}void txtR(Canvas c,String s,float x,float y,float z,int color){p.setColor(color);p.setTextSize(z);p.setTextAlign(Paint.Align.RIGHT);c.drawText(s,x,y,p);}static float dist(float a,float b,float c,float d){return (float)Math.hypot(a-c,b-d);}
        @Override public boolean onTouchEvent(MotionEvent e){float tx=e.getX(),ty=e.getY();if(e.getAction()==MotionEvent.ACTION_DOWN){if(playing){if(tx<getWidth()/2&&ty>getHeight()-180){jx=105;jy=getHeight()-88;kx=Math.max(45,Math.min(165,tx));ky=ty;}else if(tx>getWidth()-160&&ty>getHeight()-180)fire=true;}else if(win||lose){if(ty>getHeight()/2)start();}else if(ty>155&&ty<230)start();return true;}if(playing&&(e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP)){if(tx<getWidth()/2&&ty>getHeight()-210){kx=Math.max(45,Math.min(165,tx));ky=Math.max(getHeight()-155,Math.min(getHeight()-25,ty));}if(e.getAction()==MotionEvent.ACTION_UP){kx=jx;ky=jy;fire=false;}return true;}return true;}
        static class Enemy{float x,hp,max,speed;boolean boss,dead;long last,contact;}static class Shot{float x,y,vx,vy;int dmg;boolean player;}
    }
}
