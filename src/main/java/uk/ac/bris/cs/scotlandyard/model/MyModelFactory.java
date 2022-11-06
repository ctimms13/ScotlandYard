package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	private final class MyModel implements Model{
		private GameSetup setup;
		private Player mrX;
		private ImmutableList<Player> detectives;
		private ImmutableSet<Observer> observers;
		private Board.GameState gameState;

		private MyModel(GameSetup setup, Player mrX, ImmutableList<Player> detectives){
			this.setup = setup;
			this.mrX = mrX;
			this.detectives = detectives;
			this.observers = ImmutableSet.of();

			MyGameStateFactory factory = new MyGameStateFactory();
			this.gameState = factory.build(setup, mrX, detectives);
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return gameState;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException("Can't register a null observer");
			if (observers.contains(observer)) throw new IllegalArgumentException("observer already exists");
			List<Observer> observerList = new ArrayList<>(observers.asList());
			observerList.add(observer);
			observers = ImmutableSet.copyOf(observerList);
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			if (observer == null) throw new NullPointerException("Can't unregister a null observer");
			if (!observers.contains(observer)) throw new IllegalArgumentException("Observer doesn't exist");
			List<Observer> observerList = new ArrayList<>(observers.asList());
			observerList.remove(observer);
			observers = ImmutableSet.copyOf(observerList);
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return observers;
		}

		public void notifyObservers(Observer.Event event){
			for (Observer observer : observers){
				observer.onModelChanged(gameState, event);
			}
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			gameState = gameState.advance(move);
			notifyObservers(Observer.Event.MOVE_MADE);

			if (!gameState.getWinner().isEmpty()){
				notifyObservers(Observer.Event.GAME_OVER);
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
										  Player mrX,
										  ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}
}



// WHY DOESN'T THIS WORK - aidan 08/04/2021
// BECAUSE I HAVEN'T LOOKED AT IT YET - charlotte 08/04/2021
// FIXED IT - charlotte 08/04/2021