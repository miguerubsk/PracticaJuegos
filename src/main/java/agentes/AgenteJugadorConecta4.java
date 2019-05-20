/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import jade.content.ContentManager;
import jade.content.Predicate;
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
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Motivo;
import static juegosTablero.Vocabulario.Motivo.PARTICIPACION_EN_JUEGOS_SUPERADA;
import static juegosTablero.Vocabulario.getOntologia;
import juegosTablero.aplicacion.OntologiaJuegoConecta4;
import juegosTablero.aplicacion.conecta4.EstadoJuego;
import juegosTablero.aplicacion.conecta4.Ficha;
import juegosTablero.aplicacion.conecta4.JuegoConecta4;
import juegosTablero.aplicacion.conecta4.Movimiento;
import juegosTablero.aplicacion.conecta4.MovimientoEntregado;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.JuegoAceptado;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Motivacion;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;
import juegosTablero.dominio.elementos.ProponerJuego;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteJugadorConecta4 extends Agent implements Vocabulario{
    
    public static final int NULL = -1;
    public static final int CASILLA_VACIA = 0;
    public static final int CASILLA_J1 = 2;
    public static final int CASILLA_J2 = 1;
    
    // Para la generación y obtención del contenido de los mensages
    private final ContentManager manager = (ContentManager) getContentManager();
	
    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology ontologia;
    
    private Jugador jugador;
    private Juego juego;
    private Color color;
    private Random rand;
    private int estadoTablero[][];
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
	jugador = new Jugador(this.getLocalName(), this.getAID());
        rand = new Random();
        
        // Regisro de la Ontología
        try {
            ontologia = OntologiaJuegoConecta4.getInstance();
        } catch (BeanOntologyException ex) {
            Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, ex);
        }
        manager.registerLanguage(codec);
	manager.registerOntology(ontologia);
        
        //Registro en Paginas Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType(TIPO_SERVICIO);
	sd.setName(NombreServicio.JUEGO_CONECTA_4.name());
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}catch(FIPAException fe){
            fe.printStackTrace();
	}
        
        //Template para la tarea de RecepcionProposicionJuego
        MessageTemplate temp = MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_PROPOSE);
        
        //Template para la tarea JugarPartida
        MessageTemplate temp2 = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),MessageTemplate.MatchPerformative(ACLMessage.CFP));
        
        //Se añaden las tareas principales.
        addBehaviour(new TareaRecepcionProposicionJuego(this, temp));
        addBehaviour(new TareaJugarPartidaJugador(this, temp2));
    }
    
    /**
     * Finalzacion del Agente.
     */
    @Override
    protected void takeDown() {

        //Desregistro de las Páginas Amarillas
        try {
            DFService.deregister(this);
	}
            catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Se liberan los recuros y se despide
        System.out.println("Finaliza la ejecución de " + this.getName());
    }
    
    private void registrarJuego(ProponerJuego pj){
        juego = pj.getJuego();
        JuegoConecta4 jc4 = (JuegoConecta4) pj.getTipoJuego();
        estadoTablero = new int[jc4.getTablero().getDimX()][jc4.getTablero().getDimY()];
        IniciarParametros();
    }
    
    private void IniciarParametros(){
        color = null;
        ReiniciarTablero();
    }
    
    //Se marcan todas las casillas del tablero como vacias.
    public void ReiniciarTablero(){
        for(int i=0; i<estadoTablero.length; i++){
            for(int j=0; j<estadoTablero[i].length; j++){
                estadoTablero[i][j] = CASILLA_VACIA;
            }
        }
    }
    
    private Posicion calcularJugada(){
        int columna = 0, fila = NULL;
        while(fila == NULL){  //Modificar por el caclculo de la posicion.
            columna = rand.nextInt(7);
            fila = getFila(columna);
        }
        Posicion pos = new Posicion(fila, columna);
        return pos;
    }
    
    public int getFila(int columna){
        for(int i=0; i<estadoTablero.length; i++){
            if(estadoTablero[estadoTablero.length-i-1][columna] == CASILLA_VACIA){
                return estadoTablero.length-i-1;
            }
        }
        return NULL;
    }
    
    public Estado ComprobarTablero(int coordX, int coordY, int jugador){
        int coordXact, coordYact, cont;
        boolean fin;
        //Vertical.
        cont = 1;
        //Hacia arriba.
        coordXact = coordX;
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordYact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia abajo.
        coordXact = coordX;
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordYact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        
        //Horizontal
        cont = 1;
        //Hacia la izquierda.
        coordXact = coordX-1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordXact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia la derecha.
        coordXact = coordX+1; //Se evita comprobar la casilla central.
        coordYact = coordY;
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordXact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        
        //Diagonal desde la izquierda-arriba
        cont = 1;
        //Hacia arriba.
        coordXact = coordX-1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordXact--;
                coordYact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia la abajo.
        coordXact = coordX+1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordXact++;
                coordYact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        
        //Diagonal desde la derecha-abajo
        cont = 1;
        //Hacia abajo.
        coordXact = coordX-1; 
        coordYact = coordY+1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordXact--;
                coordYact++;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        //Hacia arriba.
        coordXact = coordX+1; 
        coordYact = coordY-1; //Se evita comprobar la casilla central.
        fin = false;
        while((coordXact >= 0 && coordXact < estadoTablero.length && coordYact >= 0 && coordYact < estadoTablero[0].length) && !fin){
            if((estadoTablero[coordXact][coordYact] == jugador)){
                coordXact++;
                coordYact--;
                cont++;  
            }else{
                fin = true;
            }
            if(cont==4){
                return Estado.GANADOR;
            }
        }
        return Estado.SEGUIR_JUGANDO;
    }
    
    //Tareas del Agente Jugador de Conecta 4.
    
    /**
     * Tarea que recibe la propuesta de jugar de la Central de Juegos (Protocolo Request).
     */
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
            try {
                Action ac = (Action) manager.extractContent(propose);
                ProponerJuego pj = (ProponerJuego) ac.getAction();
                if(true){
                    registrarJuego(pj);
                    ACLMessage accept = propose.createReply();
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    manager.fillContent(accept, new JuegoAceptado(pj.getJuego(), jugador));
                    return accept;
                }else{
                    ACLMessage reject = propose.createReply();
                    reject.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    manager.fillContent(reject, new Motivacion(pj.getJuego(),Motivo.PARTICIPACION_EN_JUEGOS_SUPERADA));
                    return reject;
                }
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
              
            throw new NotUnderstoodException("Remitente desconocido.\n");
        }
        
    }
    
    /**
     * Tarea que indica las jugadas al tablero (Contract Net).
     */
    public class TareaJugarPartidaJugador extends ContractNetResponder{

        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param mt Mensaje que se espera recibir.
         */
        public TareaJugarPartidaJugador(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        /**
         * Funcion que procesa un mensaje CFP y acepta o rechaza la propuesta.
         * @param cfp Mensaje que se ha recibido.
         * @return 
         * @throws RefuseException
         * @throws FailureException
         * @throws NotUnderstoodException
         */
        @Override
        protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
            //Se obtiene el contenido del mensaje del tablero.
            try {
                Action ac = (Action) manager.extractContent(cfp);
                PedirMovimiento pm = (PedirMovimiento) ac.getAction();
                //si es el primer movimiento, se toma el color para el jugador.
                if(color == null){
                    if(pm.getJugadorActivo().getAgenteJugador().getName().equals(jugador.getAgenteJugador().getName())){
                        color = Color.ROJO;
                    }else{
                        color = Color.AZUL;
                    }
                }
                //Se construye la respuesta.
                ACLMessage accept = cfp.createReply();
                accept.setPerformative(ACLMessage.PROPOSE);
                //Si es su turno.
                if(pm.getJugadorActivo().getAgenteJugador().getName().equals(jugador.getAgenteJugador().getName())){
                    //Se realiza el movimiento.
                    Movimiento movimiento = new Movimiento(new Ficha(color), calcularJugada());
                    manager.fillContent(accept, new MovimientoEntregado(pm.getJuego(), movimiento));
                }
                return accept;
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
            throw new FailureException("No se ha podido completar la jugada.");
        }
        
        /**
         * 
         * @param cfp 
         * @param propose 
         * @param accept 
         * @return 
         * @throws FailureException
         */
        @Override
        protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
            try {
                MovimientoEntregado me = (MovimientoEntregado) manager.extractContent(accept);
                Movimiento movComprobacion = new Movimiento(new Ficha(me.getMovimiento().getFicha().getColor()), new Posicion(getFila(me.getMovimiento().getPosicion().getCoorY()),me.getMovimiento().getPosicion().getCoorY()));
                estadoTablero[movComprobacion.getPosicion().getCoorX()][movComprobacion.getPosicion().getCoorY()] = me.getMovimiento().getFicha().getColor().ordinal()+1;
                //Comprobar si se continua o se ha ganado la partida
                Estado estado = ComprobarTablero(movComprobacion.getPosicion().getCoorX(), movComprobacion.getPosicion().getCoorY(), movComprobacion.getFicha().getColor().ordinal()+1);
                if(estado == Estado.GANADOR){
                    if(me.getMovimiento().getFicha().getColor() != color){
                        estado = Estado.FIN_PARTIDA;
                        System.out.println(jugador.getNombre());
                        for(int i=0; i<estadoTablero.length; i++){
                            for(int j=0; j<estadoTablero[0].length; j++){
                                System.out.print(estadoTablero[i][j]+"  ");
                            }
                            System.out.println();
                        }
                    }
                    IniciarParametros();
                }
                ACLMessage informDone = accept.createReply();
                informDone.setPerformative(ACLMessage.INFORM);
                manager.fillContent(informDone, new EstadoJuego(me.getJuego(), estado));
                return informDone;
            } catch (Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteJugadorConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
            throw new FailureException("No se ha podido completar la jugada.");
        }    
        
    }
    
}
