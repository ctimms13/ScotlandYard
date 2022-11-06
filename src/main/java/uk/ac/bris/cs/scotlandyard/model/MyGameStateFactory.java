package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Move.SingleMove;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.Move.DoubleMove;

import java.util.*;
import java.util.stream.Collectors;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {
		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableList<Player> everyone;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives){

			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			this.everyone = ImmutableList.<Player>builder().add(mrX).addAll(detectives).build();
			this.winner = ImmutableSet.copyOf(Collections.<Piece>emptySet());

			if (setup.rounds.isEmpty()){
				throw new IllegalArgumentException("Game can't be started with 0 rounds");
			} //check if the number of rounds is not 0

			if(setup.graph.edges().size() == 0){
				throw new IllegalArgumentException("Game can't be started with an empty graph");
			}

			if (mrX == null){
				throw new NullPointerException("mrX is null");
			} // Check if mrX exists

			for (Player detective : detectives){
				if (detective == null){
					throw new NullPointerException("Detective is null");
				} //check if detective exists

				if (detective.tickets().get(ScotlandYard.Ticket.DOUBLE) != 0){
					throw new IllegalArgumentException("Detective has double tickets");
				} // Make sure the detectives don't have double move tickets

				if (detective.tickets().get(ScotlandYard.Ticket.SECRET) != 0){
					throw new IllegalArgumentException("Detective has secret tickets");
				} // Make sure detectives don't have secret move tickets

				if (detective == mrX){
					throw new IllegalArgumentException("one or more mrXs");
				} // Make sure there's only one mrX, no extra X's in the detectives

				int count = 0;
				for (Player detectiveCheck : detectives){
					if (detective.location() == detectiveCheck.location()){
						count += 1;
					}
				}
				if (count > 1){
					throw new IllegalArgumentException("Two or more detectives share the same location");
				} // Check if 2 detectives have the same location
			} // Iterate through the detectives and make sure they pass tests
		}

		@Override
		public GameSetup getSetup() {return setup;}

		@Override
		public ImmutableSet<Piece> getPlayers() {
			Set<Piece> everyonePiece = everyone.stream().map(n -> n.piece()).collect(Collectors.toSet());
			return ImmutableSet.copyOf(everyonePiece);
		} // map all players to pieces and return as immutable set

		@Override
		public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			Player detectiveCheck = getPlayer(detective); //convert piece to player

			if (detectiveCheck == null){
				return Optional.empty();
			}

			return Optional.of(detectiveCheck.location());
		} // if null return empty, else return location.

		@Override
		public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			Player player = getPlayer(piece);

			if (player == null){
				return Optional.empty();
			}
			// convert piece to player and check if it exists

			return Optional.of(new TicketBoard() {
				@Override
				public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
					return player.tickets().get(ticket);
				}
			}); // genarate object based upon an annoymous innerclass
		}

		@Override
		public ImmutableList<LogEntry> getMrXTravelLog() { return log; }

		@Override
		public ImmutableSet<Piece> getWinner() {
			boolean detectivesNoTickets = true;
			for (Player detective : detectives){
				if (detective.location() == mrX.location()){
					winner = ImmutableSet.copyOf(detectives.stream().map(x -> x.piece()).collect(Collectors.toSet()));
				} // if a detective is on mrX, detectives win
				if (getTicketCount(detective) > 0) {
					detectivesNoTickets = false;
				}
			}
			if (detectivesNoTickets){
				winner = ImmutableSet.of(mrX.piece());
			} // if all detectives have no tickets, mrX wins

			int mrXSingleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location()).size();
			int mrXDoubleMoves = makeSingleMoves(setup, detectives, mrX, mrX.location()).size();

			if (mrXDoubleMoves + mrXSingleMoves == 0){
				winner = ImmutableSet.copyOf(detectives.stream().map(x -> x.piece()).collect(Collectors.toSet()));
			} // if mrX has no avalibe moves, detectives win

			if (log.size() == setup.rounds.size() & remaining.contains(mrX.piece())) {
					winner = ImmutableSet.of(mrX.piece());
			} // if the game runs out of rounds, mrX wins

			return winner;
		}

		@Override
		@Nonnull
		public ImmutableSet<Move> getAvailableMoves() {
			if (!winner.isEmpty()) return ImmutableSet.of();

			List<Move> moves = new ArrayList<Move>();

			for (Player player : this.everyone){ 																		//for all players
				if (!this.remaining.contains(player.piece())) continue;

				moves.addAll(makeSingleMoves(this.setup, this.detectives, player, player.location()));

				if (player.has(Ticket.DOUBLE)){ 																		//if the player has double tickets because mrX can run out of them
					moves.addAll(makeDoubleMoves(this.setup, this.detectives, player, player.location())); 				// add all double moves
				}
			}

			return ImmutableSet.copyOf(moves);
		}

		@Override
		public GameState advance(Move move) {
			if(!getAvailableMoves().contains(move)) throw new IllegalArgumentException("Illegal move: "+move); // removes illegal moves
			updateRemaining(move); // updates the gamestates remaining atribute to corispond with the next gamestates remaining.
			Move.Visitor<Move> visitor = new Move.Visitor<>() {
				@Override
				public SingleMove visit(SingleMove move) {
					return move;
				}

				@Override
				public DoubleMove visit(DoubleMove move) {
					return move;
				}
			}; // convert move into single or double move using visitor design pattern

			int roundNumber = log.size();
			Player newMrX = mrX;
			Player newDetective = null;
			List<Player> newDetectives = new ArrayList<>(detectives);
			List<LogEntry> newLog = new ArrayList<>(log);
			move = move.visit(visitor);

			if (move instanceof SingleMove){ // if the move is a single move
				SingleMove singleMove = (SingleMove)move;
				Player player = getPlayer(move.commencedBy());
				player = player.at(singleMove.destination);
				player = player.use(singleMove.ticket);

				if (move.commencedBy().isMrX()){
					newMrX = player;
					newLog = mrXLog(singleMove.ticket, singleMove.destination, roundNumber, newLog);
				} // update mrX's position, mrX's ticket count, and the log.
				else {
					newDetective = player;
					newMrX = newMrX.give(singleMove.ticket);
				} // update the players position and ticket count, give ticket to mrX

			} else { // if the move is a double move
				DoubleMove doubleMove = (DoubleMove) move;

				newMrX = newMrX.use(doubleMove.tickets());
				newMrX = newMrX.at(doubleMove.destination2);

				newLog = mrXLog(doubleMove.ticket1, doubleMove.destination1, roundNumber, newLog);
				newLog = mrXLog(doubleMove.ticket2, doubleMove.destination2, roundNumber+1, newLog);

				//update mrX position and update the log
			} // dont consider detectives as they can't have double tickets
			if (newDetective != null) {
				newDetectives.set(newDetectives.indexOf(getPlayer(move.commencedBy())), newDetective);
			} // add the updated detective to detectives if needed
			return new MyGameState(setup, remaining, ImmutableList.copyOf(newLog), newMrX, newDetectives); // return new gamestate
		}

		/*-------------------ALL HELPER FUNCTIONS---------------------
		  -------------------ONLY CRAZY CODE HERE---------------------
		  -------------------YOU HAVE BEEN WARNED---------------------*/

		//THE MOST IMPORTANT FUNCTION HERE
		//Retrieves the Player associated with the given piece
		private Player getPlayer(Piece piece){
			for (Player player : everyone){
				if (player.piece() == piece) return player;
			}
			return null;
		}
       //retrieve the ticket count of a player
		private int getTicketCount(Player player){
			int ticketCount = 0;
			for (int i : player.tickets().values()){
				ticketCount += i;
			}
			return ticketCount;
		}

		//Creates a new log entry for mrX using the given parameters
		private List<LogEntry> mrXLog(Ticket ticket, int destination, int roundNumber, List<LogEntry> newLog){
			if (!setup.rounds.get(roundNumber)){
				newLog.add(LogEntry.hidden(ticket));
			}
			else {
				newLog.add(LogEntry.reveal(ticket, destination));
			}
			return newLog;
		}
		//Update the remaining list based on who made the last move
		private void updateRemaining(Move move){
			if (remaining.size()-1 == 0) { 																				//if there's only one in the list
				if (move.commencedBy().isDetective()) { 																//if it's a detective
					remaining = ImmutableSet.of(mrX.piece()); 															//mrX goes next
				}
				else{
					remaining = ImmutableSet.copyOf(detectives.stream()
							.filter(player -> getTicketCount(player)>0)
							.map(player -> player.piece())
							.collect(Collectors.toList()));
				}
			}
			else {
				List<Piece> remainingList = new ArrayList<>(remaining.asList()); 										//if there's more than one in the list, make a mutable version
				remainingList.remove(move.commencedBy()); 																//remove the one who made the move
				remaining = ImmutableSet.copyOf(remainingList); 														//return an immutable set copy
			}
		}
		//Add a single move to the list of available moves
		private ArrayList<SingleMove> addMove (Player player, int source, Transport t, int destination, 				//add the single move to the list
											   ArrayList<SingleMove> singleMoves, Ticket ticket){
			if (player.has(t.requiredTicket())){
				SingleMove moveAdd = new SingleMove(player.piece(), source, ticket, destination);
				if (!singleMoves.contains(moveAdd)) singleMoves.add(moveAdd);
			}
			return singleMoves;
		}

		//Find all possible single moves for the given player
		private ImmutableSet<SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives,
														 Player player, int source){

			var singleMoves = new ArrayList<SingleMove>(); 																//creating the list to add to, mutable list

			for(int destination : setup.graph.adjacentNodes(source)) {													//for all destinations adjacent to the source
				boolean skip = false;
				for (Player detective : detectives){																	//for all players in detectives
					if (detective.location() == destination){															//if there are detectives at the location then skip that node
						skip = true;
					}
				}
				if (!skip) {
					for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {		//for transport t that the node has
						singleMoves = addMove(player, source, t, destination, singleMoves, t.requiredTicket());			// add the move to the list
					}
					if (player.has(Ticket.SECRET)){																		//if the player has secret tickets
						for (Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
							SingleMove moveAdd = new SingleMove(player.piece(), source, Ticket.SECRET, destination);	//add secret ticket  versions
							if (!singleMoves.contains(moveAdd)) singleMoves.add(moveAdd);
						}
					}
				}
			}
			return ImmutableSet.copyOf(singleMoves);
		}

		//Find all Double moves a player can make
		@Nonnull
		private ImmutableSet<DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives,
														 Player player, int source){

			final var doubleMoves = new ArrayList<DoubleMove>(); 														//creating the list to add to
			final var singleMovesSource = new ArrayList<SingleMove>(); 													//create a list of singlemoves to move from

			if (!player.has(Ticket.DOUBLE)) return ImmutableSet.copyOf(doubleMoves); //if teh

			if (this.log.size()+1 >= setup.rounds.size()) return ImmutableSet.copyOf(doubleMoves);

			singleMovesSource.addAll(makeSingleMoves(this.setup, this.detectives, player, source)); 					// add all the single moves mrX can make
			for (SingleMove moveSource : singleMovesSource){ 															//for all single moves available
				Ticket ticket = moveSource.ticket; 																		//the current ticket for the move
				int ticketAmount = player.tickets().get(ticket) - 1; 													//subtract that type of ticket
				int source2 = moveSource.destination; 																	//second node to loop through
				final var singleMovesDest = new ArrayList<SingleMove>();

				singleMovesDest.addAll(makeSingleMoves(this.setup, this.detectives, player, source2)); 					// add all the moves to the list


				for (SingleMove Dest1 : singleMovesDest){ 																//for all the first moves in the destinations of teh first nodes
					DoubleMove addThis = new DoubleMove(player.piece(), source, moveSource.ticket, source2,
							Dest1.ticket, Dest1.destination);

					if (addThis.ticket2 == ticket){ 																	//check if doublemove used the final ticket (of that type) before
						if (ticketAmount == 0) continue;																//if they don't have the ticket for that move skip it
					}
					if (!doubleMoves.contains(addThis)) doubleMoves.add(addThis);										//if the move isn't already in the set then add it
				}

			}
			return ImmutableSet.copyOf(doubleMoves);																	//return a copy of the set
		}

	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(mrX.piece()), ImmutableList.of(), mrX, detectives); //build gamestate
	}
}
