package ill.eu.rtsptofmp4.controllers;

import ill.eu.rtsptofmp4.controllers.dto.StreamConnectionDto;
import ill.eu.rtsptofmp4.models.StreamInfo;
import ill.eu.rtsptofmp4.models.StreamInit;
import ill.eu.rtsptofmp4.models.exceptions.StreamingException;
import ill.eu.rtsptofmp4.business.services.StreamService;
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
        List<StreamInfo> streams = this.streamService.getStreams();

        return streams;
    }

    @POST
    @Path("{streamId}/connect")
    public StreamInit connect(String streamId, StreamConnectionDto streamConnection) {
        if (streamConnection == null || streamConnection.getClientId() == null) {
            throw new BadRequestException("Stream connection request body does not have a clientId");
        }

        try {
            StreamInit streamInit = this.streamService.connect(streamId, streamConnection.getClientId());
            return streamInit;

        } catch (NoSuchElementException e) {
            throw new NotFoundException("Could not find stream details with Stream Id " + streamId);

        } catch (StreamingException e) {
            Log.errorf("An error occurred connecting to stream %s: %s", streamId, e.getMessage());

            this.streamService.disconnect(streamId, streamConnection.getClientId());

            throw new InternalServerErrorException(e.getMessage());
        }
    }

    @POST
    @Path("{streamId}/disconnect")
    public String disconnect(String streamId, StreamConnectionDto streamConnection) {
        if (streamConnection == null || streamConnection.getClientId() == null) {
            throw new BadRequestException("Stream disconnection request body does not have a clientId");
        }

        try {
            this.streamService.disconnect(streamId, streamConnection.getClientId());
            return "Ok";

        } catch (NoSuchElementException e) {
            throw new NotFoundException("Could not find stream details with Stream Id " + streamId);
        }
    }
}
