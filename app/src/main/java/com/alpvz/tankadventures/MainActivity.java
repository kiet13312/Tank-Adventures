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
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);setContentView(new GameView(this));}

    static class GameView extends View {
        final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); final Path path=new Path(); final Random r=new Random(); final SharedPreferences sp;
        final int[] HP={500,800,1100,1500}, COST={0,1000,2000,3000}; final float[] SPEED={4.2f,4.7f,5.2f,5.8f};
        int coins,owned=0,selected=0,level=1,wins=0,hp,chassis,armor,wheels,motor; float x,y,vx,vy,cam,tilt; boolean playing,win,lose,shop;
        long start,lastRepair,lastCollision; float joyBase=105,joyY,knobX=105,knobY;
        final ArrayList<Rival> rivals=new ArrayList<>(); final ArrayList<Part> parts=new ArrayList<>();

        GameView(Context c){super(c);sp=c.getSharedPreferences("tank_adventures",0);coins=sp.getInt("coins",0);owned=sp.getInt("owned",0);selected=Math.max(0,Math.min(3,sp.getInt("selected",0)));level=Math.max(1,Math.min(15,sp.getInt("level",1)));wins=sp.getInt("wins",0);p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);}
        void save(){sp.edit().putInt("coins",coins).putInt("owned",owned).putInt("selected",selected).putInt("level",level).putInt("wins",wins).apply();}
        @Override protected void onDraw(Canvas c){if(playing){update();drawGame(c);}else drawMenu(c);postInvalidateDelayed(16);}
        float ground(float wx){return getHeight()*.70f+(float)Math.sin(wx*.0048f)*58+(float)Math.sin(wx*.0105f+level)*22;}

        void startLevel(){playing=true;win=lose=false;shop=false;hp=HP[selected];chassis=100;armor=100;wheels=100;motor=100;x=120;y=ground(x)-45;vx=vy=0;cam=0;tilt=0;rivals.clear();parts.clear();r.setSeed(level*911L+selected*73L);for(int i=0;i<7+level/3;i++){Rival q=new Rival();q.x=900+i*470+r.nextInt(160);q.speed=1.0f+level*.025f;q.size=.9f+Math.min(.65f,level*.035f+(i%3)*.06f);q.durability=120+level*10+(i%3)*30;q.max=q.durability;rivals.add(q);}start=System.currentTimeMillis();lastRepair=start;lastCollision=0;}

        void update(){long now=System.currentTimeMillis();float dt=Math.min(2f,Math.max(.5f,(now-start)/16.666f+.5f));
            if(hp<=0||chassis<=0){lose=true;playing=false;return;}
            if(now-start>=60000){win=true;playing=false;wins++;coins+=100;if(wins>=2){wins=0;if(level<15)level++;}save();return;}
            float input=(knobX-joyBase)/58f;if(Math.abs(input)<.08f)input=0;float max=SPEED[selected]*(motor<40?.62f:1f)*(wheels<40?.68f:1f);vx+=input*.13f*dt;if(input==0)vx*=.972f;if(vx>max)vx=max;if(vx<-max*.55f)vx=-max*.55f;x+=vx*dt*1.7f;if(x<70)x=70;
            float ty=ground(x)-(wheels<=0?35:45);vy+=.48f*dt;y+=vy*dt;if(y>=ty){float impact=Math.abs(vy);y=ty;if(impact>6&&now-lastCollision>350)breakdown((int)Math.min(20,impact*1.7f));vy=-Math.min(3.8f,impact*.16f);}tilt=(float)Math.atan2(ground(x+25)-ground(x-25),50);cam+=(x-getWidth()*.30f-cam)*.11f;if(cam<0)cam=0;
            if(now-lastRepair>=4500){hp=Math.min(HP[selected],hp+12);chassis=Math.min(100,chassis+7);armor=Math.min(100,armor+10);wheels=Math.min(100,wheels+7);motor=Math.min(100,motor+7);lastRepair=now;}
            for(Rival q:rivals){if(q.removed)continue;float dx=x-q.x;float dy=y-(ground(q.x)-43*q.size);float d=Math.max(1,(float)Math.hypot(dx,dy));if(Math.abs(dx)<420)q.x+=(dx/d)*q.speed*dt;if(d<70*q.size&&now-q.lastCollision>700){int self=Math.max(3,4+level/3);int other=Math.max(6,7+level/2);hp=Math.max(0,hp-self);breakdown(self+3);q.durability-=other;q.lastCollision=now;vx+=(dx>=0?2.3f:-2.3f);vy=-3.0f;if(q.durability<=0){q.removed=true;coins+=20;save();}}}
            for(Part z:parts){z.vy+=.36f*dt;z.x+=z.vx*dt;z.y+=z.vy*dt;float gy=ground(z.x)-8;if(z.y>=gy){z.y=gy;z.vy*=-.2f;z.vx*=.82f;z.sleep=Math.abs(z.vx)<.12f&&Math.abs(z.vy)<.12f;}}
        }

        void breakdown(int n){long now=System.currentTimeMillis();lastCollision=now;chassis=Math.max(0,chassis-n);armor=Math.max(0,armor-n/2);wheels=Math.max(0,wheels-n/3);motor=Math.max(0,motor-n/4);hp=Math.max(0,hp-Math.max(1,n/4));if(armor<25&&r.nextInt(100)<14)drop(0);if(wheels<25&&r.nextInt(100)<12)drop(1);if(motor<25&&r.nextInt(100)<10)drop(2);}
        void drop(int type){for(Part z:parts)if(z.type==type&&Math.abs(z.x-x)<35)return;Part z=new Part();z.type=type;z.x=x+(r.nextBoolean()?18:-18);z.y=y-14;z.vx=(r.nextBoolean()?1:-1)*(1.2f+r.nextFloat()*1.5f);z.vy=-3-r.nextFloat();parts.add(z);}
        void repair(){if(!playing||coins<25)return;coins-=25;hp=Math.min(HP[selected],hp+90);chassis=Math.min(100,chassis+28);armor=Math.min(100,armor+32);wheels=Math.min(100,wheels+24);motor=Math.min(100,motor+24);save();}

        void drawGame(Canvas c){c.drawColor(Color.rgb(82,165,220));p.setColor(Color.rgb(125,150,86));path.reset();path.moveTo(0,getHeight());for(int i=0;i<=getWidth();i+=8)path.lineTo(i,ground(cam+i));path.lineTo(getWidth(),getHeight());path.close();c.drawPath(path,p);for(Rival q:rivals)if(!q.removed)drawRival(c,q);for(Part z:parts)drawPart(c,z);drawVehicle(c,x-cam,y,selected,1.25f,tilt,true);hud(c);controls(c);}

        void drawVehicle(Canvas c,float cx,float cy,int type,float s,float a,boolean player){c.save();c.rotate((float)Math.toDegrees(a),cx,cy);float w=84*s,h=44*s;p.setColor(Color.rgb(28,30,31));c.drawRoundRect(new RectF(cx-w/2-5,cy-h/2-2,cx+w/2+5,cy+h/2+8),9,9,p);p.setColor(player?Color.rgb(42,145,68):Color.rgb(105,107,110));c.drawRoundRect(new RectF(cx-w/2,cy-h/2,cx+w/2,cy+h/2),8,8,p);
            if(player&&armor>0){p.setColor(Color.rgb(27,93,44));c.drawRoundRect(new RectF(cx-w/2+4,cy-h/2-8,cx+w/2-8,cy-h/2+4),5,5,p);}
            int wc=player?(wheels<=0?2:4):4;p.setColor(Color.DKGRAY);for(int i=0;i<wc;i++){float wx=cx-w*.31f+i*w*.21f;c.drawCircle(wx,cy+h*.52f,10*s,p);p.setColor(Color.LTGRAY);c.drawCircle(wx,cy+h*.52f,4*s,p);p.setColor(Color.DKGRAY);}
            p.setColor(player&&motor<35?Color.rgb(100,60,50):Color.rgb(58,72,58));c.drawRect(cx-17,cy-1,cx+15,cy+14,p);p.setColor(player?Color.rgb(55,87,58):Color.rgb(70,71,73));c.drawCircle(cx,cy-8*s,17*s,p);p.setColor(Color.rgb(190,210,220));c.drawRect(cx-3,cy-31*s,cx+3,cy-11*s,p);c.drawCircle(cx,cy-33*s,5*s,p);if(player&&(motor<40||wheels<40)){p.setColor(Color.argb(130,70,70,70));c.drawCircle(cx+24*s,cy-28*s,8*s,p);}c.restore();}

        void drawRival(Canvas c,Rival q){float X=q.x-cam,Y=ground(q.x)-43*q.size;if(X<-150||X>getWidth()+150)return;drawVehicle(c,X,Y,1,q.size,0,false);p.setColor(Color.DKGRAY);c.drawRect(X-45*q.size,Y-66*q.size,X+45*q.size,Y-59*q.size,p);p.setColor(Color.rgb(255,190,70));c.drawRect(X-45*q.size,Y-66*q.size,X-45*q.size+90*q.size*(q.durability/q.max),Y-59*q.size,p);if(q.durability<=q.max*.25f){txt(c,"HỎNG",X,Y-78*q.size,12,Color.WHITE);}}
        void drawPart(Canvas c,Part z){float X=z.x-cam;if(X<-40||X>getWidth()+40)return;p.setColor(z.type==0?Color.rgb(27,93,44):z.type==1?Color.DKGRAY:Color.rgb(58,72,58));if(z.type==1)c.drawCircle(X,z.y,9,p);else c.drawRoundRect(new RectF(X-14,z.y-7,X+14,z.y+7),4,4,p);}

        void hud(Canvas c){p.setColor(Color.argb(205,20,25,28));c.drawRoundRect(new RectF(10,10,getWidth()-10,86),12,12,p);txtL(c,"MÀN "+level+"/15",22,34,16,Color.WHITE);txtL(c,"HP "+Math.max(0,hp)+"/"+HP[selected],115,34,16,Color.WHITE);txtL(c,"ĐỐI THỦ "+countAlive(),270,34,16,Color.WHITE);txtR(c,"XU "+coins,getWidth()-22,34,16,Color.WHITE);txtL(c,"KHUNG",115,58,11,Color.WHITE);txtL(c,"GIÁP",230,58,11,Color.WHITE);txtL(c,"BÁNH",340,58,11,Color.WHITE);txtL(c,"MOTOR",455,58,11,Color.WHITE);bar(c,155,51,215,57,chassis,Color.GREEN);bar(c,270,51,330,57,armor,Color.rgb(70,180,230));bar(c,390,51,450,57,wheels,Color.YELLOW);bar(c,510,51,570,57,motor,Color.rgb(220,150,70));}
        int countAlive(){int n=0;for(Rival q:rivals)if(!q.removed)n++;return n;}
        void bar(Canvas c,float l,float t,float rr,float b,int v,int color){p.setColor(Color.DKGRAY);c.drawRect(l,t,rr,b,p);p.setColor(color);c.drawRect(l,t,l+(rr-l)*v/100f,b,p);}
        void controls(Canvas c){float by=getHeight()-86;p.setColor(Color.argb(120,0,0,0));c.drawCircle(105,by,64,p);c.drawCircle(getWidth()-165,by,52,p);c.drawRoundRect(new RectF(getWidth()-275,by-33,getWidth()-190,by+33),10,10,p);c.drawRoundRect(new RectF(getWidth()-165,by-33,getWidth()-80,by+33),10,10,p);p.setColor(Color.argb(210,230,230,230));c.drawCircle(knobX,knobY==0?by:knobY,23,p);txt(c,"←  →",105,by+6,18,Color.WHITE);txt(c,"SỬA 25",getWidth()-232,by+6,13,Color.WHITE);txt(c,"GHÉP",getWidth()-122,by+6,13,Color.WHITE);}

        void drawMenu(Canvas c){c.drawColor(Color.rgb(75,155,215));txt(c,win?"THẮNG!":lose?"XE HỎNG":"TANK ADVENTURES",getWidth()/2,70,40,Color.WHITE);if(!playing&&!win&&!lose){txt(c,"XE LẮP GHÉP • ĐỐI ĐẦU TRỰC TIẾP",getWidth()/2,105,17,Color.WHITE);btn(c,getWidth()/2-160,155,getWidth()/2+160,210,"CHƠI MÀN "+level);btn(c,getWidth()/2-160,225,getWidth()/2+160,280,"CỬA HÀNG");}else btn(c,getWidth()/2-140,getHeight()/2,getWidth()/2+140,getHeight()/2+55,"CHƠI LẠI");}
        void btn(Canvas c,float l,float t,float rr,float b,String s){p.setColor(Color.argb(210,20,25,28));c.drawRoundRect(new RectF(l,t,rr,b),12,12,p);txt(c,s,(l+rr)/2,(t+b)/2+6,17,Color.WHITE);}
        void txt(Canvas c,String s,float x,float y,float z,int color){p.setColor(color);p.setTextSize(z);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}void txtL(Canvas c,String s,float x,float y,float z,int color){p.setColor(color);p.setTextSize(z);p.setTextAlign(Paint.Align.LEFT);c.drawText(s,x,y,p);}void txtR(Canvas c,String s,float x,float y,float z,int color){p.setColor(color);p.setTextSize(z);p.setTextAlign(Paint.Align.RIGHT);c.drawText(s,x,y,p);}

        @Override public boolean onTouchEvent(MotionEvent e){float tx=e.getX(),ty=e.getY();if(e.getAction()==MotionEvent.ACTION_DOWN){if(playing){float by=getHeight()-86;if(tx<getWidth()/2&&ty>getHeight()-180){joyY=by;knobX=Math.max(45,Math.min(165,tx));knobY=Math.max(by-58,Math.min(by+58,ty));}else if(tx>getWidth()-290&&tx<getWidth()-175&&ty>by-50&&ty<by+50)repair();}else if(win||lose){if(ty>getHeight()/2)startLevel();}else if(ty>145&&ty<220)startLevel();else if(ty>220&&ty<300)shop=!shop;return true;}if(playing&&(e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP)){if(tx<getWidth()/2&&ty>getHeight()-210){knobX=Math.max(45,Math.min(165,tx));knobY=Math.max(getHeight()-144,Math.min(getHeight()-28,ty));}if(e.getAction()==MotionEvent.ACTION_UP){knobX=joyBase;knobY=joyY;}return true;}return true;}

        static class Rival{float x,speed,size,durability,max;boolean removed;long lastCollision;}
        static class Part{static final int ARMOR=0,WHEEL=1,MOTOR=2;int type;float x,y,vx,vy;boolean sleep;}
    }
}
