package GUI;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import javafx.util.Pair;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import juegosTablero.Vocabulario;
import juegosTablero.Vocabulario.Estado;
import juegosTablero.aplicacion.barcos.PosicionBarcos;
import juegosTablero.dominio.elementos.Jugador;
import juegosTablero.dominio.elementos.Posicion;

/**
 *
 * @author ROBERTO
 */
public class BarquitosJFrame extends JFrame{
    
    public static final int NUM_COLUMNAS = 10;
    public static final int NUM_FILAS = 10;
    
    public static final int AGUA = 0;
    public static final int BARCO = 1;
    public static final int TOCADO = 2;
    public static final int JUGADOR_1 = 0;
    public static final int JUGADOR_2 = 1;
    public static final int NUM_JUGADORES = 2;
    
    private JPanel tablero[][][];
    private int estadoTablero[][][];
    private ImageIcon casillas[];
    private int numColumnas;
    private int numFilas;
    private ArrayList<Jugador> jugadores;
    private int puntuaciones[];
    
    public BarquitosJFrame(int _numColumnas, int _numFilas, ArrayList<Jugador> _jugadores){
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setResizable(false);
        tablero = new JPanel[NUM_FILAS][NUM_COLUMNAS][NUM_JUGADORES];
        for(int i=0; i<NUM_FILAS; i++){
            for(int j=0; j<NUM_COLUMNAS; j++){
                for (int k = 0; k < NUM_JUGADORES; k++){
                    tablero[i][j][k] = new JPanel();
                }
            }
        }
        estadoTablero = new int[NUM_FILAS][NUM_COLUMNAS][NUM_JUGADORES];
            for(int i=0; i<NUM_FILAS; i++){
                for(int j=0; j<NUM_COLUMNAS; j++){
                    for (int k = 0; k < NUM_JUGADORES; k++)
                        estadoTablero[i][j][k] = AGUA;
                }
            }
        casillas = new ImageIcon[3];
        casillas[AGUA] = new ImageIcon("src/main/java/graficos/agua.jpg");
        casillas[BARCO] = new ImageIcon("src/main/java/graficos/barco.jpg");
        casillas[TOCADO] = new ImageIcon("src/main/java/graficos/tocado.jpg");
        iniciarTablero();
        visualizarTablero();
    }

    
    public void iniciarTablero(){
        for(int i=0; i<NUM_FILAS; i++){
            for(int j=0; j<NUM_COLUMNAS; j++){
                for (int k = 0; k < NUM_JUGADORES; k++){
                tablero[i][j][k].removeAll();
                tablero[i][j][k].add(new JLabel(casillas[AGUA]));
                tablero[i][j][k].repaint();
                estadoTablero[i][j][k] = AGUA;
                }
            }
        }
    }
    
    public void colocarBarcos(Posicion coor, Vocabulario.Orientacion orientacion, int tam){
        int x = coor.getCoorX();
        int y = coor.getCoorY();
        if (orientacion == Vocabulario.Orientacion.HORIZONTAL){
            for (int i = y; i<= tam; i++){
                estadoTablero[x][i][0]= BARCO;
            }
        }else{
            for (int i = x; i<= tam; i++){
                estadoTablero[i][y][0]= BARCO;
            }
        }
    }
    
    public void visualizarTablero(){
        Container container = this.getContentPane();
        container.removeAll();
        JPanel interfaz = new JPanel();
//        interfaz.setLayout(new GridLayout(2,1));
        interfaz.setLayout(new BoxLayout(interfaz, BoxLayout.Y_AXIS));
        JPanel infoJugadores = new JPanel();
//        infoJugadores.setLayout();
        Font fuenteNombres = new Font("SansSerif", Font.BOLD, 20);
        Font fuentePuntuacion = new Font("SansSerif", Font.BOLD, 50);
        JLabel imagenFichaJ1 = new JLabel(casillas[AGUA]);
        JLabel nombreJ1 = new JLabel(jugadores.get(JUGADOR_1).getNombre());
        nombreJ1.setFont(fuenteNombres);
        JLabel puntuacionJ1 = new JLabel(Integer.toString(puntuaciones[JUGADOR_1]));
        puntuacionJ1.setFont(fuentePuntuacion);
        JLabel guion = new JLabel(" - ");
        guion.setFont(fuentePuntuacion);
        JLabel puntuacionJ2 = new JLabel(Integer.toString(puntuaciones[JUGADOR_2]));
        puntuacionJ2.setFont(fuentePuntuacion);
        JLabel nombreJ2 = new JLabel(jugadores.get(JUGADOR_2).getNombre());
        nombreJ2.setFont(fuenteNombres);
        JLabel imagenFichaJ2 = new JLabel(casillas[AGUA]);
        infoJugadores.add(imagenFichaJ1);
        infoJugadores.add(nombreJ1);
        infoJugadores.add(puntuacionJ1);
        infoJugadores.add(guion);
        infoJugadores.add(puntuacionJ2);
        infoJugadores.add(nombreJ2);
        infoJugadores.add(imagenFichaJ2);
        JPanel tableroVisual = new JPanel();
        tableroVisual.setLayout(new GridLayout(numFilas,numColumnas));
        for(int i=0; i<numFilas; i++){
            for(int j=0; j<numColumnas; j++){
                for (int k = 0; k < NUM_JUGADORES; k++)
                    tableroVisual.add(tablero[i][j][k]);
            }
        }
        interfaz.add(infoJugadores);
        interfaz.add(tableroVisual);
        container.add(interfaz);
        this.pack();
    }
    
    public void sumarVictoria(int jugador){
        puntuaciones[jugador]++;
        visualizarTablero();
    }
    
    public void marcarVictoria(String IDpartida){
        Estado cas = ComprobarEstadoTablero(IDpartida);
        
        visualizarTablero();
    }
    
    public void nuevaRonda(){
        jugadores.add(jugadores.remove(JUGADOR_1));
        int tmp = puntuaciones[JUGADOR_1];
        puntuaciones[JUGADOR_1] = puntuaciones[JUGADOR_2];
        puntuaciones[JUGADOR_2] = tmp;
        iniciarTablero();
    }
    
    
     public Vocabulario.Estado ComprobarEstadoTablero(String idPartida){
        int numTocadosJ1 = 0;
        int numTocadosJ2 = 0;
        for(int x = 0; x < 10; x++){
            for(int y = 0; y < 10; y++){
                if(tablero[x][y][1] == TOCADO) numTocadosJ1++;
                if(tablero[x][y][0] == TOCADO) numTocadosJ2++;
            }
        }
        if(numTocadosJ1 == 21){
            return Vocabulario.Estado.GANADOR;
        }else if(numTocadosJ2 == 21){
            return Vocabulario.Estado.ABANDONO;
        }else{
            return Vocabulario.Estado.SEGUIR_JUGANDO;
        }
    }    
    
    public int getPuntuacionJ1(){
        return puntuaciones[JUGADOR_1];
    }
    
    public int getPuntuacionJ2(){
        return puntuaciones[JUGADOR_2];
    }
}