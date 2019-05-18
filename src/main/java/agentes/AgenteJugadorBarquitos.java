/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import jade.proto.ProposeResponder;
import java.util.ArrayList;
import java.util.HashMap;
import jade.util.leap.List;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.ModoJuego;
import juegosTablero.Vocabulario.NombreServicio;
import juegosTablero.Vocabulario.TipoJuego;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.ColocarBarcos;
import juegosTablero.aplicacion.barcos.EstadoJuego;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.aplicacion.barcos.Localizacion;
import juegosTablero.aplicacion.barcos.MovimientoEntregado;
import juegosTablero.aplicacion.barcos.PosicionBarcos;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 *
 * @author pedroj
 */
public class AgenteJugadorBarquitos extends Agent implements Vocabulario{
    private Ontology ontologiaBarcos;
    private Jugador jugador;
    private Random rand = new Random(System.currentTimeMillis());
    private Vocabulario.Efecto resultadoAnterior;
    private final Codec codec = new SLCodec();
    private final ContentManager managerBarcos = (ContentManager) getContentManager();
    private HashMap<String, int[][][]> Tableros;
    private static final int AGUA = 0;
    private static final int BARCO = 1;
    private static final int TOCADO = 2;
    private static final int DISPARO = 3;
    ArrayList<Localizacion> localizacionBarcos[] = new ArrayList[10];
    Localizacion locations = null;
    Posicion coord = null;
    
    
    
    

    @Override
    protected void setup() {
        FileReader fr = null;
        try {
            System.out.println("Inicia la ejecución de " + this.getName());
            jugador = new Jugador(this.getLocalName(), this.getAID());
            Tableros = new HashMap<String, int[][][]>();
            for(int i=0; i<10; i++){
                localizacionBarcos[i]=new ArrayList();
            }
            fr = new FileReader("barcos.ini");
            BufferedReader bf = new BufferedReader(fr);
            String barco="";
        while ((barco = bf.readLine())!=null) {
            String[] parts = barco.split("-");
            
            coord.setCoorX(Integer.parseInt(parts[1]));
            coord.setCoorY(Integer.parseInt(parts[2]));
            locations.setPosicion(coord);
            switch(Integer.parseInt(parts[3])){
                case 1:
                    locations.setBarco(TipoBarco.FRAGATA);
                case 2:
                    locations.setBarco(TipoBarco.DESTRUCTOR);
                case 3:
                    locations.setBarco(TipoBarco.ACORAZADO);
                case 4:
                    locations.setBarco(TipoBarco.PORTAAVIONES);
            }
            switch(Integer.parseInt(parts[4])){
                case 1:
                    locations.setOrientacion(Orientacion.HORIZONTAL);
                case 2:
                    locations.setOrientacion(Orientacion.VERTICAL);
            }
            
            localizacionBarcos[Integer.parseInt(parts[0])].add(locations);
        }
            try {
                ontologiaBarcos = OntologiaJuegoBarcos.getInstance();
            } catch (BeanOntologyException ex) {
                Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
            }
            managerBarcos.registerLanguage(codec);
            managerBarcos.registerOntology(ontologiaBarcos);
            // Registro en las páginas Amarillas
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType(TIPO_SERVICIO);
            sd.setName(NombreServicio.JUEGO_BARCOS.name());
            dfd.addServices(sd);
            try {
                DFService.register(this, dfd);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            JuegoBarcos juegoBarcos = new JuegoBarcos();
            MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE),MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            MessageTemplate temp2 = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),MessageTemplate.MatchPerformative(ACLMessage.CFP));
            addBehaviour(new TareaRecepcionProposicionJuego(this, temp));
            addBehaviour(new TareaJugarPartida(this, temp2));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
	}
        
        
   

    @Override
    protected void takeDown() {
        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    public class TareaRecepcionProposicionJuego extends ProposeResponder{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param mt Mensaje que se espera recibir.
         */
        public TareaRecepcionProposicionJuego(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        @Override
        protected ACLMessage prepareResponse(ACLMessage propose) throws NotUnderstoodException, RefuseException {	
            Boolean temp = false;        
            
            
            try {
                        Action ac = (Action) managerBarcos.extractContent(propose);
                        ProponerJuego pj = (ProponerJuego) ac.getAction();
                        if(pj.getJuego().getTipoJuego()!= Vocabulario.TipoJuego.BARCOS){
                            ACLMessage refuse = propose.createReply();
                            refuse.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            managerBarcos.fillContent(refuse, new Motivacion(pj.getJuego(), Motivo.TIPO_JUEGO_NO_IMPLEMENTADO));
                            return refuse;
                        }else{
                            if(propose.getSender().equals("AgenteTablero")){
                                ACLMessage ponerBarcos = propose.createReply();
                                ponerBarcos.setPerformative(ACLMessage.PROPOSE);
                                ColocarBarcos variable = (ColocarBarcos) managerBarcos.extractContent(propose);
                                managerBarcos.fillContent(ponerBarcos, colocarBarcos(variable));
                                return ponerBarcos;
                            }else{
                                ACLMessage accept = propose.createReply();
                                accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                                managerBarcos.fillContent(accept, new JuegoAceptado(pj.getJuego(), jugador));
                                return accept;
                            }
                        }
                    } catch (Codec.CodecException | OntologyException e) {
                            e.printStackTrace();
                    } catch (IOException ex) {
                Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }
    }
    
    public class TareaJugarPartida extends ContractNetResponder{
        
        public TareaJugarPartida(Agent a, MessageTemplate mt) {
            super(a, mt);
        }
        
        protected ACLMessage HandleCfp (ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException{
            
            try{
                Action ac = (Action) managerBarcos.extractContent(cfp);
                PedirMovimiento pm = (PedirMovimiento) ac.getAction();
                
                ACLMessage accept = cfp.createReply();
                accept.setPerformative(ACLMessage.PROPOSE);
                if (pm.getJugadorActivo().getAgenteJugador().getName().equals(jugador.getAgenteJugador().getName())){
                    managerBarcos.fillContent(accept, calcularJugada(pm));
                }
                
                return accept;
                
            }catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, e);
            }
            throw new FailureException("No se ha podido completar la jugada.");
        }
        
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            try{
                MovimientoEntregado me = (MovimientoEntregado) managerBarcos.extractContent(accept);
                if(Tableros.get(me.getJuego().getIdJuego())[me.getMovimiento().getCoorX()][me.getMovimiento().getCoorY()][0] == BARCO || Tableros.get(me.getJuego().getIdJuego())[me.getMovimiento().getCoorX()][me.getMovimiento().getCoorY()][0] == TOCADO){
                    Tableros.get(me.getJuego().getIdJuego())[me.getMovimiento().getCoorX()][me.getMovimiento().getCoorY()][0] = TOCADO;
                }else{
                    Tableros.get(me.getJuego().getIdJuego())[me.getMovimiento().getCoorX()][me.getMovimiento().getCoorY()][0] = DISPARO;
                }
                
                
                Estado estado = ComprobarEstadoTablero(me.getJuego().getIdJuego());
                //TODO
                ACLMessage informDone = accept.createReply();
                informDone.setPerformative(ACLMessage.INFORM);
                managerBarcos.fillContent(informDone, new EstadoJuego(me.getJuego(), estado));
                return informDone;
                
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorBarquitos.class.getName()).log(Level.SEVERE, null, e);
            }
            throw new FailureException("No se ha podido completar la jugada.");
        }
        
    }
    
    public PosicionBarcos colocarBarcos(ColocarBarcos juego) throws FileNotFoundException, IOException{
        PosicionBarcos posiciones = new PosicionBarcos();
        posiciones.setJuego(juego.getJuego());
        
        int acceso = rand.nextInt(10);
        List listaPosiciones = new jade.util.leap.ArrayList(localizacionBarcos[acceso]);
        posiciones.setLocalizacionBarcos(listaPosiciones);
        
        int Tablero[][][] = null;
        
        for (int x=0; x<10; x++){
            for (int y =0; y<10; y++){
                for(int z = 0; z < 2; z++){
                    Tablero[x][y][z] = AGUA;
                }
            }
        }
        
        for(int i = 0; i<9; i++){
            if(localizacionBarcos[acceso].get(i).getOrientacion()==Orientacion.HORIZONTAL){
                for (int z = localizacionBarcos[acceso].get(i).getPosicion().getCoorX(); z < localizacionBarcos[acceso].get(i).getPosicion().getCoorX()+localizacionBarcos[acceso].get(i).getBarco().getCasillas(); z++){
                    Tablero[z][localizacionBarcos[acceso].get(i).getPosicion().getCoorY()][0] = BARCO;
                }
            }else{
                for(int z = localizacionBarcos[acceso].get(i).getPosicion().getCoorY(); z < localizacionBarcos[acceso].get(i).getPosicion().getCoorY()+localizacionBarcos[acceso].get(i).getBarco().getCasillas(); z++){
                    Tablero[localizacionBarcos[acceso].get(i).getPosicion().getCoorX()][z][0] = BARCO;
                }
            }
        }
        
        
        Tableros.put(juego.getJuego().getIdJuego(),Tablero);
        
        return posiciones;
    }
    
    public MovimientoEntregado calcularJugada(PedirMovimiento juego){
        
        Random rand = new Random(System.currentTimeMillis());
        int x, y;
        
        if (/**resultadoAnterior != Efecto.TOCADO*/true){
            do{
                x = rand.nextInt(10);
                y = rand.nextInt(10);
            }while(Tableros.get(juego.getJuego().getIdJuego())[x][y][1] != AGUA);
            Posicion coord = new Posicion(x, y);
            MovimientoEntregado jugada = new MovimientoEntregado(juego.getJuego(), coord);
            
            return jugada;
        }
        
        
        return null;
    }
    
    public Estado ComprobarEstadoTablero(String idPartida){
        int numTocadosJ1 = 0;
        int numTocadosJ2 = 0;
        for(int x = 0; x < 10; x++){
            for(int y = 0; y < 10; y++){
                if(Tableros.get(idPartida)[x][y][1] == TOCADO) numTocadosJ1++;
                if(Tableros.get(idPartida)[x][y][0] == TOCADO) numTocadosJ2++;
            }
        }
        if(numTocadosJ1 == 21){
            return Estado.GANADOR;
        }else if(numTocadosJ2 == 21){
            return Estado.ABANDONO;
        }else{
            return Estado.SEGUIR_JUGANDO;
        }
    }    
    
}