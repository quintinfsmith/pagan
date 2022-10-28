class NoPathGiven(Exception):
    '''
        Raised when a path is needed but not provided.
    '''

class InvalidPosition(Exception):
    '''
        Raised when a position is given to
        an OpusManager function that doesn't lead
        to a valid grouping.
    '''
