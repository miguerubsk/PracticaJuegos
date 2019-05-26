/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

import GUI.BarquitosJFrame;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
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
import jade.proto.AchieveREResponder;
import jade.proto.ContractNetInitiator;
import jade.util.leap.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Color;
import juegosTablero.Vocabulario.Estado;
import juegosTablero.Vocabulario.Puntuacion;
import juegosTablero.aplicacion.OntologiaJuegoBarcos;
import juegosTablero.aplicacion.barcos.EstadoJuego;
import juegosTablero.aplicacion.barcos.JuegoBarcos;
import juegosTablero.aplicacion.barcos.MovimientoEntregado;
import juegosTablero.dominio.elementos.ClasificacionJuego;
import juegosTablero.dominio.elementos.CompletarJuego;
import juegosTablero.dominio.elementos.Juego;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.PedirMovimiento;
import juegosTablero.dominio.elementos.Posicion;

/**
 *
 * @author Roberto Martínez Fernández
 */
public class AgenteTableroBarquitos extends Agent{
    
    
    private static final int AGUA = 0;
    private static final int BARCO = 1;
    private static final int TOCADO = 2;
    private static final int DISPARO = 3;
    public static final int TIEMPO_DE_ESPERA = 60000;
    public static final int RETARDO_MOVIMIENTOS = 2000;
    public static final int TIEMPO_JUGADA = 2000;
    public static final int JUGADOR_1 = 0;
    public static final int JUGADOR_2 = 1;
    public static final int NULL = -1;
    public static final Posicion MOV_VICTORIA = new Posicion(-1, -1);
    
    // Para la generación y obtención del contenido de los mensages
    private final ContentManager manager = (ContentManager) getContentManager();
	
    // El lenguaje utilizado por el agente para la comunicación es SL 
    private final Codec codec = new SLCodec();

    // Las ontología que utilizará el agente
    private Ontology ontologia;
    
    private BarquitosJFrame gui;
    private ArrayList<Posicion> listaMov;
    private ArrayList<Jugador> jugadores;
    private Juego juego;
    private int tablero[][];
    private int indexJugadorAct;
    private int puntuaciones[];
    private int partida;
    private int minVictorias;
    private File log;
    private boolean repeticion;
    private Posicion movAnt;
    
    /**
     * Inicializacion del Agente y las tareas iniciales.
     */
    @Override
    protected void setup() {
        
        //Incialización de variables
        listaMov = new ArrayList<>();
        jugadores = new ArrayList<>();
	Object[] args = this.getArguments();
	CompletarJuego cj = (CompletarJuego) args[0];
        jugadores = (ArrayList<Jugador>) args[2];
	juego = cj.getJuego();
        JuegoBarcos jb = (JuegoBarcos) cj.getTipoJuego();
        gui = new BarquitosJFrame(jb.getTablero().getDimY(),jb.getTablero().getDimX(), jugadores);
        gui.setVisible(true);
        tablero = new int[jb.getTablero().getDimX()][jb.getTablero().getDimY()];
        indexJugadorAct = 0; //indice del jugador que realiza la jugada actual.
        puntuaciones = new int[cj.getListaJugadores().size()];
        for(int i=0; i<cj.getListaJugadores().size(); i++){
            puntuaciones[i] = 0;
        }
        partida = (int) args[1];
        log = new File("log/"+juego.getModoJuego().name()+"_"+juego.getIdJuego()+"-Partida_"+partida+".log");
        repeticion = false;
        //Regisro de la Ontología
        
        //Registro en Página Amarillas
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
	ServiceDescription sd = new ServiceDescription();
	sd.setType("iowpengvoianpvganbanbribnoebir");
	sd.setName("aoengñvoianoerabvioearnbzspbn");
	dfd.addServices(sd);
	try {
            DFService.register(this, dfd);
	}
	catch (FIPAException fe) {
            fe.printStackTrace();
	}
        
        //Se añaden las tareas principales.
        if(log.isFile()){
            repeticion = true;
            LeerLog();
        }else{
            minVictorias = juego.getMinVictorias();
            IniciarLog();
            IniciarJugada(jugadores.get(JUGADOR_1)); 
        }
        addBehaviour(new TareaProcesarMovimiento(this,RETARDO_MOVIMIENTOS));
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
    
    
    
    /**
     * Se intercambian los jugadores para una nueva ronda.
     */
    public void NuevaRonda(){
        jugadores.add(jugadores.remove(JUGADOR_1));
        int tmp = puntuaciones[JUGADOR_1];
        puntuaciones[JUGADOR_1] = puntuaciones[JUGADOR_2];
        puntuaciones[JUGADOR_2] = tmp;
        ReiniciarTablero();
        IniciarJugada(jugadores.get(JUGADOR_1));
    }

    /**
     * Se prepara una nueva jugada
     * @param jugadorInicial Jugador que inicia la partida.
     */
    public void IniciarJugada(Jugador jugadorInicial){
        //Mensaje para la tarea JugarPartida
        ACLMessage mensaje = new ACLMessage(ACLMessage.CFP);
        mensaje.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        mensaje.setReplyByDate(new Date(System.currentTimeMillis() + TIEMPO_JUGADA));
        mensaje.setSender(this.getAID());
        mensaje.setLanguage(codec.getName());
        mensaje.setOntology(ontologia.getName());
        for(int i=0; i<jugadores.size(); i++){
            mensaje.addReceiver(jugadores.get(i).getAgenteJugador());
        }
        try{
            manager.fillContent(mensaje, new Action(this.getAID(), new PedirMovimiento(juego, jugadorInicial)));
        }catch(Codec.CodecException | OntologyException e) {
            Logger.getLogger(AgenteTableroConecta4.class.getName()).log(Level.SEVERE, null, e);
        }
        //Se añade una tarea nueva.
        addBehaviour(new TareaJugarPartidaTablero(this,mensaje));
    }

    /**
     * Se marcan todas las casillas del tablero como vacias.
     */
    public void ReiniciarTablero(){
        for(int i=0; i<tablero.length; i++){
            for(int j=0; j<tablero[i].length; j++){
                tablero[i][j] = AGUA;
            }
        }
    }
    
    /**
     * Se obtiene la primera fila libre de la columna que se pasa como parámetro.
     * @param columna Columna que se quiere comprobar.
     * @return Coordenada de la primera fila libre en la columna.
     */
    public int getFila(int columna){
        for(int i=0; i<tablero.length; i++){
            if(tablero[tablero.length-i-1][columna] == AGUA){
                return tablero.length-i-1;
            }
        }
        return NULL;
    }
    
    //Funciones para el Log de las partidas.
    
    /**
     * Se crea el fichero log y se almacenan los primeros valores.
     */
    private void IniciarLog(){
        try{
            FileWriter escritura = new FileWriter(log, true);
            escritura.write("ID:"+juego.getIdJuego()+" Partida:"+partida+"\n");
            escritura.write("MinVictorias:"+minVictorias+"\n");
            escritura.write("Jugadores:");
            for(int i=0; i<jugadores.size(); i++){
                escritura.write(jugadores.get(i).getNombre()+";");
            }
            escritura.write("\n");
            escritura.close();
        }catch(IOException e){
            System.err.println("Error en la escritura");
        }
    }
    
    /**
     * Se registra un movimiento.
     * @param mov Movimiento a registrar.
     */
    private void LogMov(Posicion mov){
        try{
            FileWriter escritura = new FileWriter(log, true);
            escritura.write(mov.getCoorX()+";"+mov.getCoorY()+"\n");
            escritura.close();
        }catch(IOException e){
            System.err.println("Error en la escritura");
        }
    }
    
    /**
     * Leer un log existente.
     */
    private void LeerLog(){
        try{
            BufferedReader lectura = new BufferedReader((new FileReader(log)));
            String linea;
            while((linea = lectura.readLine())!=null){
                String[] datos = linea.split(":");
                switch(datos[0]){
                    case "ID":
                        //No se hace nada. Es una linea informativa.
                        break;
                    case "MinVictorias":
                        //Se obtiene el minimo de victorias para ganar la partida.
                        minVictorias = Integer.parseInt(datos[1]);
                        break;
                    case "Jugadores":
                        //Se colocan los jugadores en el mismo orden que la partida original.
                        String[] jug = datos[1].split(";");
                        jugadores.clear();
                        for(int i=0; i<jug.length; i++){
                            jugadores.add(new Jugador(jug[i], new AID(jug[i], AID.ISLOCALNAME)));
                        }
                        break;
                    default:
                        //Se obtienen los movimientos para reproducir la partida.
                        String[] mov = linea.split(";");
                        Posicion movimiento;
                        movimiento = new Posicion(Integer.parseInt(mov[1]),Integer.parseInt(mov[2]));
                        listaMov.add(movimiento);
                        break;
                }
            }
        }catch (IOException e){
            System.out.println("No se puede acceder al fichero.");
        }
        
    }

    
    
    
    //Tareas del Agente Tablero de Conecta 4.
    
    /**
     * Tarea que solicita y procesa los mivimientos de los jugadores para jugar la partida (Contract Net).
     */
    public class TareaJugarPartidaTablero extends ContractNetInitiator{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea.
         * @param msg Mensaje que se enviara a los agentes receptores.
         */
        public TareaJugarPartidaTablero(Agent a, ACLMessage msg) {
            super(a, msg);
        }

        /**
         * Funcion que procesa las respuestas de los agentes involucrados en el protocolo.
         * @param responses Vector con las respuestas recibidas del resto de agentes.
         * @param acceptances Vector que almacenara las respuestas que se enviaran a los agentes.
         */
        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            MovimientoEntregado movimiento = new MovimientoEntregado();
            //Se busca el mensaje del jugador que realizo el movimiento.
            for(int i=0; i<responses.size(); i++){
                ACLMessage mensaje = (ACLMessage) responses.get(i);
                if(mensaje.getPerformative() == ACLMessage.PROPOSE && mensaje.getContent()!=null){
                    try {
                        movimiento = (MovimientoEntregado) manager.extractContent(mensaje);
                        Posicion movCompleto = new Posicion(movimiento.getMovimiento().getCoorX(),movimiento.getMovimiento().getCoorY());
                        tablero[movCompleto.getCoorX()][movCompleto.getCoorY()] = movimiento.getMovimiento().getCoorX();
                        listaMov.add(movCompleto);
                    }catch(Codec.CodecException | OntologyException e){
                        Logger.getLogger(AgenteTableroConecta4.class.getName()).log(Level.SEVERE, null, e);
                    }
                }
            }
            //Se responde a todos los jugadores con el ultimo movimiento realizado.
            try{
                for(int i=0; i<responses.size(); i++){
                    ACLMessage mensaje = (ACLMessage) responses.get(i);
                    ACLMessage respuesta = mensaje.createReply();
                    respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    manager.fillContent(respuesta, movimiento);
                    acceptances.add(respuesta);
                }  
            }catch(Codec.CodecException | OntologyException e){
                Logger.getLogger(AgenteTableroConecta4.class.getName()).log(Level.SEVERE, null, e);
            }   
        }

        /**
         * Se recibe el estado de la partida de parte de los jugadores y se actúa en consecuencia.
         * @param resultNotifications Mensajes de los jugadores con el esatdo de la partida.
         */
        @Override
        protected void handleAllResultNotifications(Vector resultNotifications) {
            boolean continuar = true;
            for(int i=0; i<resultNotifications.size(); i++){
                ACLMessage result = (ACLMessage) resultNotifications.get(i);
                if(result.getPerformative() == ACLMessage.INFORM){
                    try{
                        EstadoJuego ej = (EstadoJuego) manager.extractContent(result);
                        if(ej.getEstadoJuego() == Estado.GANADOR){
                            continuar = false;
                            listaMov.add(MOV_VICTORIA);
                        }
                    }catch(Codec.CodecException | OntologyException e){
                        Logger.getLogger(AgenteTableroConecta4.class.getName()).log(Level.SEVERE, null, e);
                    }
                }else if(result.getPerformative() == ACLMessage.FAILURE){
                    //Se ha acabado la apartida y no hay ganador.
                }
            }
            if(continuar){
                //Se reinicia la tarea.
                indexJugadorAct = (indexJugadorAct+1)%jugadores.size();
                IniciarJugada(jugadores.get(indexJugadorAct));
            }else{
                puntuaciones[indexJugadorAct]++;
                if(puntuaciones[indexJugadorAct] == minVictorias){
                    //Fin de la partida. Ya hay un ganador.
                    //Los resultados se enviaran al GrupoJuegos al finalizar la representacion (Tarea ProcesarMovimiento).
                }else{
                    //Se juega una nueva ronda.
                    NuevaRonda();
                }
            }
        }
        
    }
    
    /**
     * Tarea que envia los resulatados de la partida al agente GrupoJuegos (Protocolo Request).
     */
    public class TareaInformarResultados extends AchieveREResponder{
        
        /**
         * Constructor de la tarea.
         * @param a Agente que invoco la tarea. 
         * @param mt Mensaje que se espera recibir.
         */
        public TareaInformarResultados(Agent a, MessageTemplate mt) {
            super(a, mt);
        }

        /**
         * Funcion que procesa un mensaje de solicitud.
         * @param request Mensaje con la solicitud.
         * @return Mensaje de aceptacion.
         * @throws NotUnderstoodException
         * @throws RefuseException
         */
        @Override
        protected ACLMessage handleRequest(ACLMessage request) throws NotUnderstoodException, RefuseException {
            ACLMessage agree = request.createReply();
            agree.setPerformative(ACLMessage.AGREE);
            List jugadoresClasificacion = new jade.util.leap.ArrayList();
            List puntuacionesClasificacion = new jade.util.leap.ArrayList();
            for(int i=0; i<jugadores.size(); i++){
                jugadoresClasificacion.add(jugadores.get(i));
                if(puntuaciones[i] == minVictorias){
                    puntuacionesClasificacion.add(Puntuacion.VICTORIA.getValor());
                }else{
                    puntuacionesClasificacion.add(Puntuacion.DERROTA.getValor());
                }
            }
            try{
                manager.fillContent(agree, new ClasificacionJuego(juego, jugadoresClasificacion, puntuacionesClasificacion));
            }catch(Codec.CodecException | OntologyException e) {
                Logger.getLogger(AgenteTableroConecta4.class.getName()).log(Level.SEVERE, null, e);
            }
            return agree;
        }
        
        /**
         * Funcion que prepara y envia la respuesta a la solicitud
         * @param request Mensaje con la solicitud.
         * @param response Mensaje de informacion para responder.
         * @return Mensaje con la respuesta.
         * @throws FailureException
         */
        @Override
        protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
            ACLMessage inform = request.createReply();
            inform.setPerformative(ACLMessage.INFORM);
            addBehaviour(new TareaFinalizarAgente(myAgent,TIEMPO_DE_ESPERA));
            return inform;
        }
          
    }
    
    /**
     * Tarea que actualiza la interfaz con los movimientos efectuados.
     */
    public class TareaProcesarMovimiento extends TickerBehaviour {

        /**
         * Inicializacion de la tarea.
         * @param a Agente que invoca la tarea.
         * @param period Periodo que trasncurre entre ejecuciones de la tarea.
         */
        public TareaProcesarMovimiento(Agent a, long period) {
            super(a, period);
        }
    
        /**
         * Se seleciona el siguiente movimiento de la lista (si hay) y se actualiza la interfaz.
         */
        @Override
        public void onTick(){
            if(!listaMov.isEmpty()){
                Posicion movAct = listaMov.remove(0);
                if(!repeticion){
                  LogMov(movAct);  
                }
                //Si no es el movimiento de Victoria.
                if((movAct.getCoorX() != MOV_VICTORIA.getCoorX()) && (movAct.getCoorY() != MOV_VICTORIA.getCoorY())){
                    //Se actualiza la interfaz.
                    //gui.colocarBarcos(movAct, Vocabulario.Orientacion.HORIZONTAL, int tam = 5);
                    movAnt = movAct;
                //Si es el movimiento de Victoria.
                }else{
                    //Se señala la victoria en la interfaz.
                    //gui.marcarVictoria(movAnt.getCoorX(), movAnt.getCoorY(), movAnt.getFicha().getColor().ordinal()+1);
                    //gui.sumarVictoria((movAnt.getFicha().getColor().ordinal()+1)%2);
                    //Si ya hay un ganador.
                    if(gui.getPuntuacionJ1() == minVictorias || gui.getPuntuacionJ2() == minVictorias){
                        if(repeticion){
                            puntuaciones[JUGADOR_1] = gui.getPuntuacionJ1();
                            puntuaciones[JUGADOR_2] = gui.getPuntuacionJ2();
                        }
                        //Se finaliza la partida.
                        MessageTemplate temp = MessageTemplate.and(MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                        addBehaviour(new TareaInformarResultados(myAgent,temp));
                    //Si aun no hay ganador.
                    }else{
                        //Se inicia una nueva ronda.
                        gui.nuevaRonda();
                    }
                }
            }
        }
    
    }
    
   /**
     * Tarea que prepara las puntuaciones y crea la tarea que informara al GrupoJuegos.
     */
    public class TareaFinalizarAgente extends WakerBehaviour{
        
        /**
         * Inicializacion de la tarea.
         * @param a Agente que invoca la tarea.
         * @param timeout Tiempo que debe transcurrir para que se ejecute la tarea.
         */
        public TareaFinalizarAgente(Agent a, long timeout) {
            super(a, timeout);
        }

        /**
         * Se prepara el mensaje con las puntuaciones para el GrupoJuegos.
         */
        @Override
        protected void onWake() {
            //Se finaliza al agente
            gui.dispose();
            myAgent.doDelete();
        }
        
        
    }
    
}

