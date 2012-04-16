import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.powerbot.concurrent.Task;
import org.powerbot.concurrent.strategy.Condition;
import org.powerbot.concurrent.strategy.Strategy;
import org.powerbot.game.api.ActiveScript;
import org.powerbot.game.api.Manifest;
import org.powerbot.game.api.methods.Walking;
import org.powerbot.game.api.methods.Widgets;
import org.powerbot.game.api.methods.input.Keyboard;
import org.powerbot.game.api.methods.input.Mouse;
import org.powerbot.game.api.methods.interactive.NPCs;
import org.powerbot.game.api.methods.interactive.Players;
import org.powerbot.game.api.methods.node.Menu;
import org.powerbot.game.api.methods.node.SceneEntities;
import org.powerbot.game.api.methods.tab.Inventory;
import org.powerbot.game.api.methods.widget.Camera;
import org.powerbot.game.api.util.Random;
import org.powerbot.game.api.util.Time;
import org.powerbot.game.api.wrappers.Area;
import org.powerbot.game.api.wrappers.interactive.NPC;
import org.powerbot.game.api.wrappers.node.Item;
import org.powerbot.game.api.wrappers.widget.WidgetChild;
import org.powerbot.game.api.wrappers.Tile;
import org.powerbot.game.bot.event.listener.PaintListener;

@Manifest(
		authors = "Qubiz",
		name = "QVialFiller",
		description = "fills vials at the grand-exchange",
		version = 0.1,
		website = "http://www.google.nl/"
		)

public class QVialFiller extends ActiveScript implements PaintListener{

	private final int VIAL_ID = 229;
	private final int VIAL_OF_WATER_ID = 227;
	
	private final int FOUNTAIN_ID = 47150;
	
	private final Tile BANK_TILES[] = {new Tile(3149, 3482, 0), new Tile(3151, 3482,0), new Tile(3152, 3480 ,0), new Tile(3151, 3483, 0)};
	private final Tile FOUNTAIN_TILE[] = {new Tile(3162, 3489, 0), new Tile(3162, 3491, 0), new Tile(3164, 3489, 0), new Tile(3162, 3489, 0)};
	
	private Area fountainArea = new Area(new Tile(3160, 3486, 0), new Tile(3169, 3497, 0));
	private Area bankArea = new Area(new Tile(3154, 3484, 0), new Tile(3145, 3473, 0));
	
	private boolean filling = false;
	private boolean buyNewVials = false;
	
	private int amountFilled = 0;
	
	private String status = "";
	
	private static Font verdana = new Font("Verdana", Font.BOLD, 18);
	
	@Override
	protected void setup() {	
		
		FillVials fv = new FillVials();
		DoBank db = new DoBank();
		WalkToFountain wtf = new WalkToFountain();
		WalkToBank wtb = new WalkToBank();
	
		Strategy s1 = new Strategy(fv, fv);
		Strategy s2 = new Strategy(db, db);
		Strategy s3 = new Strategy(wtf, wtf);
		Strategy s4 = new Strategy(wtb, wtb);
		
		provide(s1);
		provide(s2);
		provide(s3);
		provide(s4);
		
	}

	private class FillVials implements Task, Condition {

		@Override
		public boolean validate() {
			return inventoryContains(VIAL_ID) && fountainArea.contains(Players.getLocal().getLocation());
		}

		@Override
		public void run() {
			if(SceneEntities.getNearest(FOUNTAIN_ID) != null) {
				if(SceneEntities.getNearest(FOUNTAIN_ID).isOnScreen()) {
					if(!filling) {
						if(Inventory.selectItem(VIAL_ID)) {
							Time.sleep(Random.nextInt(300, 500));
							if(SceneEntities.getNearest(FOUNTAIN_ID).click(true)) {
								while(Inventory.getCount(VIAL_ID) != 0) {
									status = "Filling vials...";
									
									int i = Random.nextInt(0, 15);
									
									switch(i) {
										case 1: 
											Camera.setAngle(Random.nextInt(0, 500));
											Camera.setPitch(Random.nextInt(40, 120));
											break;
										
										default:
											Time.sleep(Random.nextInt(800, 1400));
											break;
									}
								}
							}
							filling = false;
						}
					}
				}
			}
		}
	}

	private class DoBank implements Task, Condition {

		@Override
		public boolean validate() {
			return bankArea.contains(Players.getLocal().getLocation()) && !inventoryContains(VIAL_ID) && !buyNewVials;
		}
		
		@Override
		public void run() {
	
			while(!Bank.isOpen()) {
				status = "Opening bank...";
				Bank.open();
				Time.sleep(Random.nextInt(700, 1300));
			}
			
			while(Inventory.getCount() > 0) {
				amountFilled += Inventory.getCount(VIAL_OF_WATER_ID);
				Bank.depositAll();
				Time.sleep(Random.nextInt(500,700));
			}
			
			if(Bank.getItemWidgetChild(VIAL_ID) != null) {
				if(Bank.withdraw(VIAL_ID, 0)) {
					Time.sleep(Random.nextInt(300, 500));
				} else {
					log.info("No vials in the bank!");
					status = "Stopping script...";
					stop();
				}
			}
				
			while(Bank.isOpen()) {
				status = "Closing bank...";
				Bank.close();
				Time.sleep(Random.nextInt(700, 1300));
			}
			
		}	
	}

	private class WalkToFountain implements Task, Condition {

		@Override
		public boolean validate() {
			return inventoryContains(VIAL_ID) && !fountainArea.contains(Players.getLocal().getLocation()) && !Bank.isOpen();
		}

		@Override
		public void run() {
			
			if(!Walking.isRunEnabled()) {
				if(Walking.getEnergy() > 30) {
					Walking.setRun(true);
				}
			}
			if(Walking.walk(FOUNTAIN_TILE[Random.nextInt(0,3)])) {
				while(Players.getLocal().isMoving()) {
					status = "Walking to the fountain...";
				}
			}
			
		}
	}

	private class WalkToBank implements Task, Condition {

		@Override
		public boolean validate() {
			return !inventoryContains(VIAL_ID) && (!bankArea.contains(Players.getLocal().getLocation()) || buyNewVials);
		}

		@Override
		public void run() {
			
			if(!Walking.isRunEnabled()) {
				if(Walking.getEnergy() > 30) {
					Walking.setRun(true);
				}
			}
			if(Walking.walk(BANK_TILES[Random.nextInt(0,3)])) {
				while(Players.getLocal().isMoving()) {
					status = "Walking to bank...";
				}
			}
			
		}
	}

//	private class BuyNewVials implements Task, Condition {
//		
//		@Override
//		public boolean validate() {
//			return buyNewVials && bankArea.contains(Players.getLocal().getLocation());
//		}
//		
//		@Override
//		public void run() {
//			
//			// code to sell filled vials and to buy new ones...
//			
//			
//		}
//	}
	
	private static class Bank {
		
		private static final int WIDGET_BANK = 762;
		private static final int WIDGET_CHILD_DEPOSIT_ALL = 34;
		private static final int WIDGET_CHILD_CLOSE = 45;
		private static final int WIDGET_BANK_INVENTORY = 95;
		private static final int[] NPC_ID_BANKERS = {3416};

		
		public static boolean isOpen() {
			return Widgets.get(WIDGET_BANK, 1).isOnScreen();
		}
		
		public static boolean open() {
			final NPC banker = NPCs.getNearest(NPC_ID_BANKERS);
			return banker != null && banker.interact("Bank");
		}
		
		public static boolean depositAll() {
			return isOpen() && Widgets.get(WIDGET_BANK).getChild(WIDGET_CHILD_DEPOSIT_ALL).click(true);
		}
		
		public static boolean withdraw(final int itemId, int amount) {
			final WidgetChild item = getItemWidgetChild(itemId);
	           	if(item != null) {
	           		final int bankSlotX = Widgets.get(WIDGET_BANK).getChild(WIDGET_BANK_INVENTORY).getRelativeX() + item.getRelativeX();
	                final int bankSlotY = Widgets.get(WIDGET_BANK).getChild(WIDGET_BANK_INVENTORY).getRelativeY() + item.getRelativeY();
	                if(Mouse.click(bankSlotX + (item.getWidth() / 2), bankSlotY + (item.getHeight() / 2), false)) {
	                    if(amount > 0) {
	                        if(Menu.isOpen()) {
	                        	if(Menu.select("Withdraw-X")) {
	                        		Keyboard.sendText(Integer.toString(amount), true);
	                        		return true;
	                        	}
	                        		
	                        }
	                   
	                    } else {
	                    	return Menu.isOpen() && Menu.select("Withdraw-All");
	                    }
	                }
	            }        
	            return false;
	    }
		
		private static WidgetChild getItemWidgetChild(int id) {
	            WidgetChild item = null;
	            for(WidgetChild i:Widgets.get(WIDGET_BANK).getChild(WIDGET_BANK_INVENTORY).getChildren())
	            {
	                if(new Item(i).getId() == id)
	                {
	                    item = i;
	                    break;
	                }
	            }
	            return item;
        }
		
		private static boolean close() {
			return Widgets.get(WIDGET_BANK).getChild(WIDGET_CHILD_CLOSE).click(true);
		}
			
	}
	
	private boolean inventoryContains(int itemId) {
		for(Item i : Inventory.getItems()) {
			if(i.getId() == itemId) {
				return true;
			}
		}
		return false;
	}

	
	@Override
	public void onRepaint(final Graphics render) {
		
		Graphics2D g2d = (Graphics2D)render;
		g2d.setFont(verdana);
		g2d.setColor(Color.CYAN);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g2d.drawString("Status: " + status, 14, 325);
		g2d.drawString("Vials filled: " + amountFilled, 14, 300);
		
	}
	
}
