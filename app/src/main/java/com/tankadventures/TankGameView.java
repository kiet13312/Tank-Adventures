package com.tankadventures;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class TankGameView extends View {
    static final int VW=1280, VH=720;
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random random=new Random();
    final ArrayList<Projectile> shots=new ArrayList<>();
    Tank player, enemy;
    float scale, ox, oy, aim=18;
    long last, enemyNext;
    boolean aimUp, aimDown;

    public TankGameView(Context c){
        super(c); setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        player=new Tank(230,515,false); enemy=new Tank(1030,500,true);
        last=System.currentTimeMillis(); enemyNext=last+1400;
    }

    @Override protected void onDraw(Canvas out){
        scale=Math.min(getWidth()/1280f,getHeight()/720f);
        ox=(getWidth()-VW*scale)/2f; oy=(getHeight()-VH*scale)/2f;
        out.save(); out.translate(ox,oy); out.scale(scale,scale);
        background(out);
        long now=System.currentTimeMillis(); float dt=Math.min(.033f,(now-last)/1000f); last=now;
        update(dt,now);
        player.draw(out); enemy.draw(out);
        for(Projectile s:shots)s.draw(out);
        ui(out);
        out.restore(); postInvalidateDelayed(16);
    }

    void update(float dt,long now){
        if(aimUp)aim=Math.min(75,aim+45*dt);
        if(aimDown)aim=Math.max(5,aim-45*dt);
        for(Projectile s:shots){
            if(s.dead)continue;
            s.vy+=760*dt; s.x+=s.vx*dt; s.y+=s.vy*dt; s.life-=dt;
            if(s.x<0||s.x>VW||s.y>610||s.life<=0){s.dead=true;continue;}
            Tank target=s.owner==player?enemy:player;
            if(target.hp>0&&target.hit(s.x,s.y)){
                s.dead=true; applyDamage(target,s.damage);
            }
        }
        if(now>enemyNext&&enemy.hp>0&&player.hp>0){
            float dx=player.cx()-enemy.cx(), dy=(player.cy()-65)-(enemy.cy()-75);
            float a=(float)Math.atan2(dy,dx);
            shots.add(Projectile.fire(enemy.cx()-20,enemy.cy()-78,a,590,24,enemy));
            enemyNext=now+1800+random.nextInt(1100);
        }
        Iterator<Projectile> it=shots.iterator(); while(it.hasNext())if(it.next().dead)it.remove();
    }

    // Tank Adventures damage model: raw attack -> armor mitigation -> HP.
    void applyDamage(Tank t,int attack){
        int absorbed=Math.min(t.armor,Math.round(attack*.45f));
        t.armor-=absorbed;
        int hpDamage=Math.max(1,attack-absorbed);
        t.hp=Math.max(0,t.hp-hpDamage);
    }

    void fire(){
        if(player.hp<=0)return;
        float a=(float)Math.toRadians(-aim);
        shots.add(Projectile.fire(player.cx()-15,player.cy()-78,a,650,28,player));
    }

    void background(Canvas c){
        p.setStyle(Paint.Style.FILL); p.setColor(Color.rgb(72,154,198)); c.drawRect(0,0,VW,720,p);
        p.setColor(Color.rgb(102,178,85)); c.drawRect(0,520,VW,720,p);
        p.setColor(Color.rgb(174,220,111)); c.drawRect(0,520,VW,548,p);
        p.setColor(Color.argb(85,255,255,255));
        c.drawCircle(165,105,35,p);c.drawCircle(210,92,48,p);c.drawCircle(258,108,32,p);
        c.drawCircle(845,130,35,p);c.drawCircle(895,118,52,p);c.drawCircle(950,135,31,p);
    }

    void ui(Canvas c){
        p.setTypeface(Typeface.DEFAULT_BOLD); p.setColor(Color.WHITE); p.setTextSize(28);
        c.drawText("TANK ADVENTURES",28,40,p);
        c.drawText("HP "+player.hp+"   ARMOR "+player.armor,28,76,p);
        c.drawText("ENEMY "+enemy.hp,1050,40,p);
        p.setColor(Color.argb(190,30,30,30)); c.drawRoundRect(30,585,165,685,18,18,p);
        p.setColor(Color.WHITE);p.setTextSize(22);c.drawText("AIM",80,616,p);c.drawText((int)aim+"°",80,650,p);c.drawText("UP / DOWN",48,678,p);
        p.setColor(Color.argb(215,190,45,35));c.drawCircle(1150,635,72,p);
        p.setColor(Color.WHITE);p.setTextSize(27);c.drawText("FIRE",1113,645,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(Color.argb(110,255,255,255));
        float a=(float)Math.toRadians(-aim);float sx=player.cx()-15,sy=player.cy()-78;
        c.drawLine(sx,sy,sx+(float)Math.cos(a)*170,sy+(float)Math.sin(a)*170,p);p.setStyle(Paint.Style.FILL);
        if(enemy.hp<=0){p.setTextSize(60);c.drawText("VICTORY!",515,300,p);}
        if(player.hp<=0){p.setTextSize(60);c.drawText("DEFEAT",540,300,p);}
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;
        if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){
            aimUp=y<520&&x>700&&x<930; aimDown=y<520&&x>=930&&x<1110;
            if(e.getAction()==MotionEvent.ACTION_DOWN&&x>1060&&y>555)fire();
            return true;
        }
        aimUp=aimDown=false; return true;
    }

    static class Projectile{
        float x,y,vx,vy,life=5;int damage;boolean dead;Tank owner;
        static Projectile fire(float x,float y,float a,float speed,int damage,Tank owner){
            Projectile z=new Projectile();z.x=x;z.y=y;z.vx=(float)Math.cos(a)*speed;z.vy=(float)Math.sin(a)*speed;z.damage=damage;z.owner=owner;return z;
        }
        void draw(Canvas c){if(dead)return;p.setColor(Color.argb(130,255,180,50));c.drawCircle(x,y,16,p);p.setColor(Color.rgb(250,235,100));c.drawCircle(x,y,9,p);}
    }

    class Tank{
        float x,y;boolean enemyTank;int hp=240,armor=120;
        Tank(float x,float y,boolean e){this.x=x;this.y=y;enemyTank=e;}
        float cx(){return x;}float cy(){return y;}
        boolean hit(float px,float py){return(px>x-120&&px<x+120&&py>y-65&&py<y+35)||(px>x-50&&px<x+65&&py>y-115&&py<y-50);}
        void draw(Canvas c){
            p.setColor(Color.argb(65,0,0,0));c.drawOval(x-145,y+22,x+145,y+65,p);
            p.setColor(enemyTank?Color.rgb(62,52,52):Color.rgb(48,55,62));c.drawRoundRect(x-145,y-20,x+145,y+55,28,28,p);
            p.setColor(Color.rgb(135,135,135));for(int i=0;i<7;i++)c.drawCircle(x-105+i*35,y+20,13,p);
            p.setColor(enemyTank?Color.rgb(125,75,60):Color.rgb(60,105,130));
            Path h=new Path();h.moveTo(x-120,y-55);h.lineTo(x+100,y-55);h.lineTo(x+125,y+5);h.lineTo(x-105,y+5);h.close();c.drawPath(h,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setColor(Color.rgb(205,205,180));c.drawLine(x-105,y-40,x+98,y-40,p);c.drawLine(x-92,y-8,x+90,y-8,p);p.setStyle(Paint.Style.FILL);
            p.setColor(enemyTank?Color.rgb(100,58,50):Color.rgb(50,78,103));c.drawRoundRect(x-50,y-108,x+60,y-50,22,22,p);
            p.setStrokeWidth(17);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(Color.rgb(45,45,45));c.drawLine(x+12,y-78,x+(enemyTank?145:110),y-78,p);
            p.setColor(Color.rgb(190,190,175));c.drawCircle(x+(enemyTank?112:-112),y-15,22,p);
            p.setColor(Color.argb(160,0,0,0));c.drawRect(x-120,y-150,x+120,y-136,p);p.setColor(Color.rgb(70,220,85));c.drawRect(x-120,y-150,x-120+240*(Math.max(0,hp)/240f),y-136,p);
            p.setColor(Color.WHITE);p.setTextSize(18);c.drawText(enemyTank?"ENEMY":"YOU",x-28,y+90,p);
        }
    }
}
