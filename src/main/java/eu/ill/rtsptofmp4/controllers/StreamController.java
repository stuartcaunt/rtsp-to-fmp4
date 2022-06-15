package eu.ill.rtsptofmp4.controllers;

import eu.ill.rtsptofmp4.controllers.dto.StreamConnectionDto;
import eu.ill.rtsptofmp4.controllers.dto.StreamDisconnectionDto;
import eu.ill.rtsptofmp4.models.StreamInfo;
import eu.ill.rtsptofmp4.models.StreamInit;
import eu.ill.rtsptofmp4.models.exceptions.StreamingException;
import eu.ill.rtsptofmp4.business.services.StreamService;
import io.quarkus.logging.Log;

import javax.inject.Inject;
import javax.ws.rs.*;
import java.util.List;
import java.util.NoSuchElementException;

@Path("/streams")
public class StreamController {

    @Inject
    StreamService streamService;

    @GET
    public List<StreamInfo> getStreams() {
        List<StreamInfo> streams = this.streamService.getAllStreamInfos();

        return streams;
    }

    @POST
    @Path("/connect")
    public StreamInit connect(StreamConnectionDto streamConnection) {
        if (streamConnection == null) {
            throw new BadRequestException("Request body does not contain stream connection data");
        }

        if (streamConnection.getId() == null || streamConnection.getName() == null || streamConnection.getUrl() == null) {
            throw new BadRequestException("Stream connection request body does not have valid stream data");
        }

        if (streamConnection.getClientId() != null && this.streamService.hasClient(streamConnection.getClientId())) {
            throw new BadRequestException("Stream connection request uses clientId that is already connected");
        }

        try {
            StreamInfo streamInfo = new StreamInfo(streamConnection.getId(), streamConnection.getName(), streamConnection.getUrl());
            StreamInit streamInit = this.streamService.connect(streamInfo, streamConnection.getClientId());
            return streamInit;

        } catch (StreamingException e) {
            Log.errorf("An error occurred connecting to stream %s: %s", streamConnection.getId(), e.getMessage());

            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @POST
    @Path("/disconnect")
    public String disconnect(StreamDisconnectionDto streamDisconnection) {
        if (streamDisconnection == null || streamDisconnection.getClientId() == null) {
            throw new BadRequestException("Stream disconnection request body does not have a clientId");
        }

        String clientId = streamDisconnection.getClientId();
        if (!this.streamService.hasClient(clientId)) {
            throw new NotFoundException("%s is not associated to any streams" + clientId);
        }

        try {
            this.streamService.disconnect(clientId);
            return "Ok";

        } catch (Exception e) {
            throw new InternalServerErrorException("An error occurred disconnecting client " + clientId + ": " + e.getMessage());
        }
    }
}
