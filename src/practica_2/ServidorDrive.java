
package practica_2;
import  java.net.*;
import java.io.*;


/**
 *
 * @author cesar
 * @author Sam
 */
public class ServidorDrive {
    public static void main(String[] args) {
        try {
            // Creación del socket UDP que escucha en el puerto 1234
            int puerto = 1234;
            DatagramSocket socketServidor = new DatagramSocket(puerto);
            System.out.println("Servidor iniciado en el puerto " + puerto + ", esperando mensajes...");

            while (true) {
                // Buffer para recibir datos (tamaño máximo de un paquete UDP)
                byte[] buffer = new byte[65535];
                DatagramPacket paquete = new DatagramPacket(buffer, buffer.length);

                // Recibir paquete
                socketServidor.receive(paquete);

                // Convertir los datos recibidos a una cadena de texto
                String mensaje = new String(paquete.getData(), 0, paquete.getLength());
                System.out.println("Mensaje recibido de " + paquete.getAddress() + ":" + paquete.getPort() + " -> " + mensaje);

                // Responder al cliente (ACK)
                String respuesta = "ACK: Mensaje recibido";
                byte[] datosRespuesta = respuesta.getBytes();
                DatagramPacket paqueteRespuesta = new DatagramPacket(datosRespuesta, datosRespuesta.length, paquete.getAddress(), paquete.getPort());
                socketServidor.send(paqueteRespuesta);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}